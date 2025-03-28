package com.snhu.cs360.inventoryapp;

import android.Manifest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //
    // Author:   Samuel Gomez
    // Company:  Southern New Hampshire University
    // Course:   CS-360
    //

    private ListView listView;
    private GridView gridView;

    private InventoryDatabaseHelper inventoryDbHelper;
    private static final int SMS_PERMISSION_CODE = 100;


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

        // Instantiate database helper to interact with SQLite databases for inventory
        inventoryDbHelper = new InventoryDatabaseHelper(this);

        // Get views future use
        listView = findViewById(R.id.listView);
        gridView = findViewById(R.id.gridView);

        // Initial populate
        displayDatabaseItems();

        // On FAB press, show dialogue to add item to inventory
        findViewById(R.id.fab_main).setOnClickListener(v -> showAddItemDialog());

    }


    /**
     * Inflates the options menu and customizes the menu item icons. This includes
     * setting up the menu layout and applying a white tint to the specified menu item icon.
     *
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
     *
     * @param item The {@link android.view.MenuItem} that was selected.
     * @return A boolean indicating whether the event was handled. Returns {@code true} if
     *         the event was consumed, otherwise invokes the superclass implementation.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        switch (String.valueOf(item.getTitle())) {
            case "Switch Layouts":
                if (listView.getVisibility() == View.VISIBLE) {
                    listView.setVisibility(View.GONE);
                    gridView.setVisibility(View.VISIBLE);
                    displayDatabaseItems();
                } else {
                    gridView.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                    displayDatabaseItems();
                }
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
     *
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
     * Displays the inventory items stored in the database within a user interface
     * as either a list or a grid, based on the current visibility state of the layout.
     *
     * The method performs the following steps:
     * 1. Retrieves all inventory items from the database using the inventory database helper.
     * 2. Maps the database columns to corresponding views using a mapping of column names
     *    (e.g., item name, description, and quantity) to view IDs.
     * 3. Checks the current visibility state of the `listView` and `gridView` to determine
     *    whether to display the items in a list or grid layout.
     * 4. Creates an adapter (InventoryAdapter) to populate the data from the retrieved
     *    Cursor into the specified layout type.
     * 5. Sets the adapter to the appropriate view, either `listView` or `gridView`, based on the layout mode.
     *
     * This method relies on the following:
     * - `inventoryDbHelper`: A helper instance to perform database operations.
     * - `listView` and `gridView`: UI components to present the data in different configurations.
     * - `InventoryAdapter`: A custom adapter that binds the database data to the respective views.
     *
     * Note: This method updates the view dynamically based on the visibility of the components
     * and does not assume a fixed layout state.
     */
    private void displayDatabaseItems() {
        // Get all inventory items
        Cursor cursor = inventoryDbHelper.getAllInventoryItems();

        // Map db columns to view
        String[] fromColumns = {"item_name", "item_description", "item_quantity"};
        int[] toViews = {R.id.itemNameTextView, R.id.itemDescriptionTextView, R.id.itemQuantityTextView};

        // Check what layout is visible - list or grid
        int layoutId = R.layout.item_view_list;
        if (listView.getVisibility() == View.GONE) {
            layoutId = R.layout.item_view_grid;
        }

        // Create adapter to display each item received by the cursor
        InventoryAdapter adapter = new InventoryAdapter(this, layoutId, cursor, fromColumns,
                toViews, 0, inventoryDbHelper);

        // Set data to listView, everything in adapter
        if (listView.getVisibility() == View.VISIBLE) {
            listView.setAdapter(adapter);
        } else {
            gridView.setAdapter(adapter);
        }

    }

    private void initSearchView() {
        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String search) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String search) {
                return false;
            }
        });
    }


    /**
     * Checks whether the user is currently logged in by retrieving the login status
     * from shared preferences.
     *
     * @return {@code true} if the user is logged in; {@code false} otherwise.
     */
    private boolean isUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_logged_in", false);
    }


    /**
     * Logs the user out of the application by clearing the user session and navigating
     * back to the login screen.
     *
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
     *
     * The dialog includes input fields for the item name and quantity and validates that both fields
     * are filled before allowing the user to save the data. If valid inputs are provided, the item is
     * added to the inventory database. The method then updates the corresponding view to reflect the
     * change in the inventory.
     *
     * The dialog has the following options:
     * - "OK" button: Saves the item to the database and updates the view. Shows a toast message if input is invalid.
     * - "Cancel" button: Dismisses the dialog without making any changes.
     *
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

                String itemName = inputName.getText().toString();
                String itemDescription = "";
                if (!inputDescription.getText().toString().isEmpty()) {
                    itemDescription = inputDescription.getText().toString();
                }

                int itemQuantity = Integer.parseInt(inputQuantity.getText().toString());
                inventoryDbHelper.addItem(itemName, itemDescription, itemQuantity);
                displayDatabaseItems();
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