package com.snhu.cs360.inventoryapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.snhu.cs360.inventoryapp.auth.LoginActivity;
import com.snhu.cs360.inventoryapp.firebase.FirebaseDatabaseHelper;
import com.snhu.cs360.inventoryapp.inventory.InventoryAdapter;
import com.snhu.cs360.inventoryapp.inventory.InventoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {


    /*
     *
     * Author:   Samuel Gomez
     * Company:  Southern New Hampshire University
     * Course:   CS-360
     *
     */


    protected RecyclerView recyclerView;
    protected InventoryAdapter inventoryAdapter;
    private List<InventoryItem> inventoryItemList;
    private List<String> tagList;
    private FirebaseDatabaseHelper firebaseDbHelper;

    private ItemTouchHelper itemTouchHelper;
    public static boolean sortAscending = false;

    protected boolean isListView = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is logged in, if not redirect to login activity
        if (!isUserLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Properly set custom toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // Initialize search toolbar
        initSearchView();

        // Instantiate database helper to interact with FB Realtime databases for inventory
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("inventory");
        firebaseDbHelper = new FirebaseDatabaseHelper(databaseReference);

        // Set RecyclerView and lists
        recyclerView = findViewById(R.id.recyclerView);
        inventoryItemList = new ArrayList<>();
        tagList = new ArrayList<>();

        // Initial layout manager and adapter setup
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryAdapter = new InventoryAdapter(inventoryItemList, isListView, firebaseDbHelper);
        recyclerView.setAdapter(inventoryAdapter);

        // Initial populate
        loadInventory();
        // Setup swipe-to-delete functionality
        setUpItemTouchHelper();

        // On FAB press, show dialogue to add item to inventory
        findViewById(R.id.fab_main).setOnClickListener(v -> showAddItemDialog());

    }


    /**
     * Inflates the options menu and customizes the menu item icons. This includes
     * setting up the menu layout and applying a white tint to the specified menu item icon.
     * <p>
     * @param menu The menu into which the items are placed.
     * @return A boolean value indicating whether the menu should be displayed. Returns
     *         {@code true} if the menu should be displayed, otherwise calls the superclass implementation.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dialog_main_menu, menu);

        // Steps to set color white, extract icon and tint it
        Drawable drawable = menu.findItem(R.id.activity_main_menu).getIcon();
        drawable = DrawableCompat.wrap(drawable);

        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.white));
        menu.findItem(R.id.activity_main_menu).setIcon(drawable);

        return super.onCreateOptionsMenu(menu);
    }


    /**
     * Handles the selection of menu items in the options menu. Executes specific actions
     * based on the selected item's title, such as logging out or toggling the user interface layout.
     * <p>
     * @param item The {@link MenuItem} that was selected.
     * @return A boolean indicating whether the event was handled. Returns {@code true} if
     *         the event was consumed, otherwise invokes the superclass implementation.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (String.valueOf(item.getTitle())) {
            case "Search and Filters":
                LinearLayout filterView = findViewById(R.id.filterView);
                if (filterView.getVisibility() == View.GONE) {
                    filterView.setVisibility(View.VISIBLE);
                } else {
                    filterView.setVisibility(View.GONE);
                }
                break;

            case "Sort by Item Name":
                sortAscending = !sortAscending;
                setSortOrder(sortAscending);
                break;

            case "Switch Layouts":
                toggleLayoutManager();
                break;

            case "Logout":
                logout();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Loads the inventory data from the Firebase database.
     * <p>
     * This method fetches inventory items from the Firebase database using the
     * FirebaseDbHelper class. The data is retrieved asynchronously through a
     * ValueEventListener. It populates the inventory item list and updates the
     * associated adapter upon successful data retrieval. If the data loading
     * process fails, an error message is displayed to the user via a Toast.
     * <p>
     * The following tasks are performed within the method: <p>
     * - Clears the current inventory item list to avoid duplicated entries. <p>
     * - Iterates through the data snapshot returned from the Firebase database. <p>
     * - Converts each snapshot to an InventoryItem instance and assigns the
     *   snapshot key as the item ID. <p>
     * - Adds the populated InventoryItem objects to the inventory list. <p>
     * - Refreshes the associated adapter to display the updated data in the UI. <p>
     * Displays an error notification if there is a failure to fetch data from the
     * database.
     */
    private void loadInventory() {
        firebaseDbHelper.fetchItems(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                inventoryItemList.clear();
                tagList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    InventoryItem item = itemSnapshot.getValue(InventoryItem.class);
                    if (item != null) {
                        // Set item id from key to populate missing id data
                        item.setId(itemSnapshot.getKey());
                        if (!tagList.contains(item.getTag())) {
                            tagList.add(item.getTag());
                        }
                        inventoryItemList.add(item);
                    }
                }

                inventoryAdapter.setItems(new ArrayList<>(inventoryItemList));
                initSpinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Toggles the layout mode of the `RecyclerView` between list view and grid view.
     * <p>
     * This method switches the current layout manager and updates the adapter to reflect
     * the selected layout mode. It also stores the user's layout preference persistently
     * in shared preferences to retain the selection across application sessions.
     * <p>
     * The method performs the following steps: <p>
     * 1. Toggles the `isListView` field, indicating the current layout mode. <p>
     * 2. Updates the shared preferences with the new layout mode. <p>
     * 3. Calls the `setLayoutManager` method to adjust the `RecyclerView`'s layout manager
     *    based on the updated `isListView` value. <p>
     * 4. Instantiates a new `InventoryAdapter` with the updated layout mode and sets it
     *    to the `RecyclerView`. <p>
     * <p>
     * Note:
     * - Shared preferences key `view_preference` is used to save and retrieve the user's layout preference.
     * - The `InventoryAdapter` requires the inventory data and current layout mode to initialize.
     */
    protected void toggleLayoutManager() {
        isListView = !isListView;

        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("view_preference", isListView);
        editor.apply();

        setLayoutManager();
        inventoryAdapter = new InventoryAdapter(inventoryItemList, isListView, firebaseDbHelper);
        recyclerView.setAdapter(inventoryAdapter);
    }


    /**
     * Configures the layout manager for the RecyclerView based on the current layout mode.
     * <p>
     * This method checks the `isListView` field to determine whether the RecyclerView
     * should use a linear or grid layout. It then sets an appropriate instance of
     * `LinearLayoutManager` or `GridLayoutManager` to the RecyclerView.
     * <p>
     * Behavior:
     * - If `isListView` is true, the RecyclerView is configured with a `LinearLayoutManager`
     *   to display items in a vertical list format.
     * - If `isListView` is false, a `GridLayoutManager` is applied with 2 columns to display
     *   items in a grid format.
     * <p>
     * Dependencies:
     * - `recyclerView` is the RecyclerView instance associated with the activity.
     * - `isListView` determines the layout mode (list or grid).
     * <p>
     * Note:
     * Ensure the RecyclerView and its associated layout state are properly initialized before
     * invoking this method.
     */
    private void setLayoutManager() {
        if (isListView) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        }
    }


    /**
     * Initializes the search functionality by configuring a {@link SearchView} to filter and
     * display inventory items in real-time based on user input.
     * <p>
     * This method sets an {@link android.widget.SearchView.OnQueryTextListener} on the SearchView widget.
     * When the user types into the SearchView, the method dynamically filters the inventory items
     * using the input text. The filtered items are displayed in the {@link RecyclerView} by updating
     * the associated adapter.
     * <p>
     * Behavior:
     * - The {@link android.widget.SearchView.OnQueryTextChange} callback is triggered as the user types. <p>
     * - The method performs a case-insensitive comparison between the input text and the names of
     *   inventory items to determine matching items. <p>
     * - A new {@link InventoryAdapter} instance is created with the filtered list and is set to the
     *   RecyclerView to display the matching results immediately. <p>
     * <p>
     * Note:
     * - This method assumes that `recyclerView` and `inventoryItemList` are properly initialized
     *   before invocation. It uses these fields to update the list dynamically in response to the
     *   search query. <p>
     * - The search functionality does not depend on whether the layout mode is a list or grid, as
     *   it uses the current layout mode indicated by `isListView`. <p>
     */
    private void initSearchView() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String search) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String search) {
                ArrayList<InventoryItem> seachedItemList = new ArrayList<>();
                for (InventoryItem item : inventoryItemList) {
                    if (item.getName().toLowerCase().contains(search.toLowerCase())) {
                        seachedItemList.add(item);
                    }
                }
                inventoryAdapter.setItems(seachedItemList);
                return false;
            }
        });
    }


    /**
     * Applies a filter to the inventory data based on the given category. This method
     * utilizes the inventory adapter to display only the inventory items that match
     * the specified category criteria.
     * <p>
     * @param category The category to filter the inventory items by. Only items
     *                 belonging to this category will be displayed.
     */
    private void applyFilter(String category) {
        if (category.equals("All")) {
            resetFilter();
            return;
        }

        ArrayList<InventoryItem> filteredItemList = new ArrayList<>();
        for (InventoryItem item : inventoryItemList) {
            if (item.getTag() != null && item.getTag().equalsIgnoreCase(category)) {
                filteredItemList.add(item);
            }
        }
        inventoryAdapter.setItems(filteredItemList);
    }


    /**
     * Resets the inventory filter and displays the full list of inventory items in the RecyclerView.
     * <p>
     * This method creates a new instance of the {@code InventoryAdapter} with the full inventory data and
     * the current layout mode. The adapter is then set to the {@code recyclerView}, effectively clearing
     * any applied filters or search results.
     * <p>
     * Behavior:
     * - Utilizes the {@code InventoryAdapter} to rebind the complete inventory list. <p>
     * - Maintains the current layout mode (list or grid) as indicated by the {@code isListView} parameter. <p>
     * <p>
     * Dependencies:
     * - {@code recyclerView}: The RecyclerView instance to display inventory items. <p>
     * - {@code inventoryItemList}: The list containing all inventory items to be displayed. <p>
     * - {@code isListView}: A boolean determining the current layout mode. <p>
     * <p>
     * Note:
     * - This method should be called when the filter has to be cleared and the entire inventory needs to
     *   be displayed. <p>
     */
    public void resetFilter() {
        inventoryAdapter.setItems(new ArrayList<>(inventoryItemList));
    }


    /**
     * Sets the sort order for the inventory list and updates the adapter with the sorted order.
     *
     * @param ascending a boolean indicating whether the inventory list should be sorted
     *                  in ascending order (true) or descending order (false)
     */
    private void setSortOrder(boolean ascending) {

        ArrayList<InventoryItem> sortedItemList = new ArrayList<>();
        if (ascending) {
            sortedItemList.addAll(inventoryItemList.stream().sorted().collect(Collectors.toList()));
        } else {
            sortedItemList.addAll(inventoryItemList.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
        }

        inventoryAdapter.setItems(sortedItemList);
    }


    /**
     * Configures and attaches an {@link ItemTouchHelper} to the provided {@link RecyclerView}.
     * This helper allows swipe gestures on list items to enable deletion of items, with added confirmation
     * and cancellation handling for improved user experience.
     * <p>
     * The method sets up swipe behavior and defines the visual and functional response
     * when an item is swiped. Swiping an item to the left displays a confirmation dialog
     * where users can approve or cancel the deletion, and employs reverse animation
     * for the cancellation action. A red background with "Delete" text provides visual
     * feedback during the swipe gesture.
     * <p>
     * Functional Overview:
     * - Confirmation Dialog: Prompts the user to confirm before deleting the item. <p>
     * - Swipe Cancellation: Uses reverse animation for smooth interface restoration if the deletion is canceled. <p>
     * - No Drag & Drop Support: Drag and drop movements are disabled by returning false in onMove(). <p>
     * - Swipe Gesture: Allows left-swipe gestures on items. <p>
     * - Custom Swipe UI: Displays a red background with a white "Delete" text as swipe feedback. <p>
     */
    private void setUpItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Get the position of the item
                int position = viewHolder.getAdapterPosition();
                InventoryItem itemToDelete = inventoryAdapter.getItemAt(position);

                // Perform deletion
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Delete Item");
                builder.setMessage("Are you sure you want to delete \"" + itemToDelete.getName() + "\"?");

                builder.setPositiveButton("Yes", (dialog, which) -> {
                    firebaseDbHelper.deleteItem(itemToDelete.getId());
                    inventoryItemList.removeIf( v -> v.getId().equals(itemToDelete.getId()));
                    inventoryAdapter.removeItem(itemToDelete);
                });

                builder.setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                    recyclerView.post(() -> {
                        // Reverse animation added for visual appeal
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                        if (holder != null) {
                            View itemView = holder.itemView;
                            ObjectAnimator animator = ObjectAnimator.ofFloat(itemView, "translationX", itemView.getTranslationX(), 0);
                            animator.setDuration(250);
                            animator.start();
                        }

                        inventoryAdapter.notifyItemChanged(position);
                        itemTouchHelper.attachToRecyclerView(null);
                        itemTouchHelper.attachToRecyclerView(recyclerView);
                    });
                });

                builder.setCancelable(false);
                builder.show();
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                inventoryAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }

            /*
             * Swipe background for the inventory list. "DELETE"
             */
            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Check if the end-user is swiping
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;

                    // Set background color for swipe (red)
                    Paint paint = new Paint();
                    paint.setColor(getResources().getColor(R.color.itemSwipetoDeleteColor));

                    // Draw the red background
                    canvas.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(),
                            (float) itemView.getBottom(), paint);

                    // "Delete" text section
                    Paint textPaint = new Paint();
                    textPaint.setColor(getResources().getColor(android.R.color.white));
                    textPaint.setTextSize(48);
                    textPaint.setTextAlign(Paint.Align.LEFT);

                    float xOffset = itemView.getRight() - 200;
                    float yOffset = (float) (itemView.getTop() + itemView.getBottom()) / 2; // Center vertically
                    canvas.drawText("Delete", xOffset, yOffset, textPaint);
                }

                // Call parent class behavior
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };

        // Attach the ItemTouchHelper to the RecyclerView
        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    /**
     * Checks whether the user is currently logged in by retrieving the login status
     * from shared preferences.
     * <p>
     * @return {@code true} if the user is logged in; {@code false} otherwise.
     */
    private boolean isUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_logged_in", false);
    }


    /**
     * Logs the user out of the application by clearing the user session and navigating
     * back to the login screen.
     * <p>
     * This method performs the following actions:
     * 1. Accesses the shared preferences to retrieve the user session.
     * 2. Clears all stored session data.
     * 3. Navigates to the {@link LoginActivity}.
     * 4. Ends the current activity to prevent the user from returning to it through the back stack.
     */
    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * Displays a dialog to add a new item to the inventory.
     * <p>
     * The dialog includes input fields for the item name and quantity and validates that both fields
     * are filled before allowing the user to save the data. If valid inputs are provided, the item is
     * added to the inventory database. The method then notifies the adapter of the change in item count.
     * <p>
     * The dialog has the following options:
     * - "OK" button: Saves the item to the database and updates the view. Shows a toast message if input is invalid.
     * - "Cancel" button: Dismisses the dialog without making any changes.
     * <p>
     * Error handling: If an exception occurs while adding the item, the dialog is dismissed, and a
     * toast message is displayed to indicate an error.
     */
    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Add New Item");

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_add_item, findViewById(android.R.id.content), false);
        final EditText inputName = viewInflated.findViewById(R.id.input_item_name);
        final EditText inputDescription = viewInflated.findViewById(R.id.input_item_description);
        final EditText inputQuantity = viewInflated.findViewById(R.id.input_item_quantity);
        final EditText inputTag = viewInflated.findViewById(R.id.input_item_tag);

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            // Name and quantity must be filled in
            if (inputName.getText().toString().isEmpty() || inputQuantity.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please enter item name and quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add new item with name, description, and quantity, and reload 'current' view
            try {
                dialog.dismiss();

                InventoryItem newItem = new InventoryItem();
                newItem.setName(inputName.getText().toString());
                newItem.setQuantity(Integer.parseInt(inputQuantity.getText().toString()));
                newItem.setTag(inputTag.getText().toString());

                String itemDescription = "";
                if (!inputDescription.getText().toString().isEmpty()) { itemDescription = inputDescription.getText().toString(); }
                newItem.setDescription(itemDescription);

                String itemTag = "Other";
                if (!inputTag.getText().toString().isEmpty()) { itemTag = inputTag.getText().toString(); }
                newItem.setDescription(itemTag);

                firebaseDbHelper.addItem(newItem);
                inventoryItemList.add(newItem);

                inventoryAdapter.notifyItemInserted(inventoryItemList.size() - 1);

                Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                dialog.dismiss();
                Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();

    }


    /**
     * Initializes the {@code Spinner} used for filtering inventory items by tags.
     * <p>
     * This method creates a dropdown menu, populating it with available tags and the "All" option
     * to reset the filter. Each tag in the `Spinner` corresponds to a category in the inventory.
     * When a user selects a tag, the inventory list is filtered based on the selected tag.
     * <p>
     * Behavior: <p>
     * - "All" resets the filter and displays all inventory items. <p>
     * - Selecting a specific tag filters the inventory to match the selected category. <p>
     * <p>
     * Steps: <p>
     * - The tag list is first updated with the "All" option at index 0. <p>
     * - An {@link ArrayAdapter} is configured with tagList and applied to the {@link Spinner}. <p>
     * - An {@link android.widget.AdapterView.OnItemSelectedListener} is set to listen and respond
     * to item selection events. It calls {@code applyFilter()} with the selected tag to filter
     * the inventory list. <p>
     * <p>
     * Note: Ensure that `tagList` contains the relevant tags and the {@code Spinner} component
     * is properly initialized in the corresponding layout resource.
     */
    private void initSpinner() {
        Spinner spinner = findViewById(R.id.tagListSpinner);
        tagList.add(0, "All");

        // Create ArrayAdapter for Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tagList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter to the Spinner
        spinner.setAdapter(adapter);

        // Implement logic when an item is selected from the Spinner to filter the inventory
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTag = parent.getItemAtPosition(position).toString();
                applyFilter(selectedTag);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

}