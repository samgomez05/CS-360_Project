package com.snhu.cs360.inventoryapp;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

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


    private RecyclerView recyclerView;
    private InventoryAdapter inventoryAdapter;
    private List<InventoryItem> inventoryItemList;
    private FirebaseDatabaseHelper firebaseDbHelper;

    private boolean isListView = true;
    private static final int SMS_PERMISSION_CODE = 100;
    private String filterCategory = "all";

    public static boolean sortAscending = false;


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

        // Request SMS and Notification permissions from user
        // Needed as an array as only one permission was being asked upon login
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), SMS_PERMISSION_CODE);
        }

        // Get views future use
        recyclerView = findViewById(R.id.recyclerView);
        inventoryItemList = new ArrayList<>();

        // Initial layout manager and adapter setup
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryAdapter = new InventoryAdapter(inventoryItemList, isListView);
        recyclerView.setAdapter(inventoryAdapter);

        // Instantiate database helper to interact with FB Realtime databases for inventory
        firebaseDbHelper = new FirebaseDatabaseHelper("inventory");

        // Initial populate
        loadInventory();
        // Setup swipe-to-delete functionality
        setUpItemTouchHelper();

        // On FAB press, show dialogue to add item to inventory
        findViewById(R.id.fab_main).setOnClickListener(v -> showAddItemDialog());

        // Link buttons in view with actions
        findViewById(R.id.filterByElectronics).setOnClickListener(v -> applyFilter("electronics"));
        findViewById(R.id.filterByOffice).setOnClickListener(v -> applyFilter("stationery"));
        findViewById(R.id.resetFilter).setOnClickListener(v -> resetFilter());

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
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
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
     * @param item The {@link android.view.MenuItem} that was selected.
     * @return A boolean indicating whether the event was handled. Returns {@code true} if
     *         the event was consumed, otherwise invokes the superclass implementation.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        switch (String.valueOf(item.getTitle())) {
            case "Search and Filters":
                SearchView searchView = findViewById(R.id.searchView);
                LinearLayout filterView = findViewById(R.id.filterView);
                if (searchView.getVisibility() == View.GONE) {
                    searchView.setVisibility(View.VISIBLE);
                    filterView.setVisibility(View.VISIBLE);
                } else {
                    searchView.setVisibility(View.GONE);
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
     * This method is called when the user grants or denies the requested permissions; it
     * checks if the SMS permission was granted and notifies the user via a
     * toast message accordingly.
     * <p>
     * @param requestCode The request code passed in {@code requestPermissions}.
     * @param permissions The requested permissions array.
     * @param grantResults The grant result status for each corresponding permission,
     *                     either {@link PackageManager#PERMISSION_GRANTED} or
     *                     {@link PackageManager#PERMISSION_DENIED}.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
     * The following tasks are performed within the method:
     * - Clears the current inventory item list to avoid duplicated entries.
     * - Iterates through the data snapshot returned from the Firebase database.
     * - Converts each snapshot to an InventoryItem instance and assigns the
     *   snapshot key as the item ID.
     * - Adds the populated InventoryItem objects to the inventory list.
     * - Refreshes the associated adapter to display the updated data in the UI.
     * <p>
     * Displays an error notification if there is a failure to fetch data from the
     * database.
     */
    private void loadInventory() {
        firebaseDbHelper.fetchItems(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                inventoryItemList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    InventoryItem item = itemSnapshot.getValue(InventoryItem.class);
                    if (item != null) {
                        // Set item id from key to populate missing id data
                        item.setId(itemSnapshot.getKey());
                        inventoryItemList.add(item);
                    }
                }

                inventoryAdapter.notifyDataSetChanged();
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
     * The method performs the following steps:
     * 1. Toggles the `isListView` field, indicating the current layout mode.
     * 2. Updates the shared preferences with the new layout mode.
     * 3. Calls the `setLayoutManager` method to adjust the `RecyclerView`'s layout manager
     *    based on the updated `isListView` value.
     * 4. Instantiates a new `InventoryAdapter` with the updated layout mode and sets it
     *    to the `RecyclerView`.
     * <p>
     * Note:
     * - Shared preferences key `view_preference` is used to save and retrieve the user's layout preference.
     * - The `InventoryAdapter` requires the inventory data and current layout mode to initialize.
     */
    private void toggleLayoutManager() {
        isListView = !isListView;

        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("view_preference", isListView);
        editor.apply();

        setLayoutManager();
        inventoryAdapter = new InventoryAdapter(inventoryItemList, isListView);
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
     * - The {@link android.widget.SearchView.OnQueryTextChange} callback is triggered as the user types.
     * - The method performs a case-insensitive comparison between the input text and the names of
     *   inventory items to determine matching items.
     * - A new {@link InventoryAdapter} instance is created with the filtered list and is set to the
     *   RecyclerView to display the matching results immediately.
     * <p>
     * Note:
     * - This method assumes that `recyclerView` and `inventoryItemList` are properly initialized
     *   before invocation. It uses these fields to update the list dynamically in response to the
     *   search query.
     * - The search functionality does not depend on whether the layout mode is a list or grid, as
     *   it uses the current layout mode indicated by `isListView`.
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
                ArrayList<InventoryItem> filteredItemList = new ArrayList<>();
                for (InventoryItem item : inventoryItemList) {
                    if (item.getName().toLowerCase().contains(search.toLowerCase())) {
                        filteredItemList.add(item);
                    }
                }
                InventoryAdapter adapter = new InventoryAdapter(filteredItemList, isListView);
                recyclerView.setAdapter(adapter);

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
        filterCategory = category;

        ArrayList<InventoryItem> filteredItemList = new ArrayList<>();
        for (InventoryItem item : inventoryItemList) {
            if (item.getTag() == null) {
                item.setTag("");
            }

            if (item.getTag().toLowerCase().contains(filterCategory.toLowerCase())) {
                filteredItemList.add(item);
            }

            InventoryAdapter adapter = new InventoryAdapter(filteredItemList, isListView);
            recyclerView.setAdapter(adapter);
        }
    }


    /**
     * Resets the inventory filter and displays the full list of inventory items in the RecyclerView.
     * <p>
     * This method creates a new instance of the {@code InventoryAdapter} with the full inventory data and
     * the current layout mode. The adapter is then set to the {@code recyclerView}, effectively clearing
     * any applied filters or search results.
     * <p>
     * Behavior:
     * - Utilizes the {@code InventoryAdapter} to rebind the complete inventory list.
     * - Maintains the current layout mode (list or grid) as indicated by the {@code isListView} parameter.
     * <p>
     * Dependencies:
     * - {@code recyclerView}: The RecyclerView instance to display inventory items.
     * - {@code inventoryItemList}: The list containing all inventory items to be displayed.
     * - {@code isListView}: A boolean determining the current layout mode.
     * <p>
     * Note:
     * - This method should be called when the filter has to be cleared and the entire inventory needs to
     *   be displayed.
     */
    public void resetFilter() {
        InventoryAdapter adapter = new InventoryAdapter(inventoryItemList, isListView);
        recyclerView.setAdapter(adapter);
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

        InventoryAdapter adapter = new InventoryAdapter(sortedItemList, isListView);
        recyclerView.setAdapter(adapter);

    }


    /**
     * Configures and attaches an {@link ItemTouchHelper} to the provided {@link RecyclerView}.
     * This helper allows swipe gestures on list items to enable deletion of items.
     * <p>
     * The method sets up swipe behavior and defines the visual and functional response
     * when an item is swiped. Swiping an item to the left triggers item deletion and displays
     * a red background with a "Delete" text for visual feedback.
     * <p>
     * Functional Overview:
     * - No Drag & Drop Support: Drag and drop movements are disabled by returning false in onMove().
     * - Swipe Gesture: Allows left-swipe gestures on items.
     * - Item Deletion: Removes the item from the underlying dataset upon swipe.
     * - Custom Swipe UI: Displays a red background with a white "Delete" text as swipe feedback.
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

                // Perform deletion
                deleteItem(position);
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

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                inventoryAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }


        };

        // Attach the ItemTouchHelper to the RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    /**
     * Deletes an item from the inventory at the specified position after prompting
     * for user confirmation. If the user confirms, the item is removed from the list,
     * the associated data is deleted from the database, and the UI is updated.
     * If the user cancels, the item's state is restored.
     * <p>
     * @param position The position of the item to be deleted in the inventory list.
     */
    private void deleteItem(int position) {
        // Get the item to delete
        InventoryItem itemToDelete = inventoryItemList.get(position);

        // Show a confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"" + itemToDelete.getName() + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    // Remove item from list
                    inventoryItemList.remove(position);
                    inventoryAdapter.notifyItemRemoved(position);
                    firebaseDbHelper.deleteItem(itemToDelete.getId());

                    // Show confirmation
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User canceled deletion, restore the item
                    inventoryAdapter.notifyItemChanged(position);
                })
                .setCancelable(false) // Prevent dismissing the dialog by tapping outside
                .show();
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

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_add_item, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText inputName = viewInflated.findViewById(R.id.input_item_name);
        final EditText inputDescription = viewInflated.findViewById(R.id.input_item_description);
        final EditText inputQuantity = viewInflated.findViewById(R.id.input_item_quantity);

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

                String itemDescription = "";
                if (!inputDescription.getText().toString().isEmpty()) { itemDescription = inputDescription.getText().toString(); }
                newItem.setDescription(itemDescription);

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

}