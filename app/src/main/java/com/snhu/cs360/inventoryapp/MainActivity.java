package com.snhu.cs360.inventoryapp;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
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

    private LoginDatabaseHelper loginDbHelper;
    private InventoryDatabaseHelper inventoryDbHelper;
    private static final int SMS_PERMISSION_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private LinearLayout linearLayout;
    private GridLayout gridLayout;


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

        // Instantiate database helpers to interact with SQLite databases for login and inventory
        loginDbHelper = new LoginDatabaseHelper(this);
        inventoryDbHelper = new InventoryDatabaseHelper(this);

        // Get views and tag for future use
        linearLayout = findViewById(R.id.list_view_layout);
        linearLayout.setTag("item_view_list");

        gridLayout = findViewById(R.id.grid_view_layout);
        gridLayout.setTag("item_view_grid");

        // Initial populate
        populateListViewWithInventoryItems(linearLayout);

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
            case "Logout":
                logout();
                break;

            case "Switch Layouts":
                toggleView();
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
     * Toggles the visibility of the user interface between two layouts: a linear layout
     * and a grid layout. Based on the current visible layout, the method hides it and
     * displays the other layout, while also populating the newly displayed layout with
     * inventory items.
     *
     * The method checks the visibility of the linear layout. If it is visible, it replaces
     * it with the grid layout, and vice versa. The appropriate layout is populated with
     * inventory items using the {@code populateListViewWithInventoryItems(ViewGroup view)}
     * method.
     */
    private void toggleView() {
        int visibleView = linearLayout.getVisibility();
        if (visibleView == View.VISIBLE) {
            linearLayout.setVisibility(View.GONE);
            gridLayout.setVisibility(View.VISIBLE);
            populateListViewWithInventoryItems(gridLayout);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
            gridLayout.setVisibility(View.GONE);
            populateListViewWithInventoryItems(linearLayout);
        }
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
     * Creates a custom view for an inventory item using the provided database cursor and layout identifier.
     * Populates the view with item details such as name and quantity, and defines interactive logic for
     * incrementing and decrementing the item quantity.
     *
     * @param cursor The {@link Cursor} object containing the data for the inventory item.
     *               Must include columns "item_name" and "quantity".
     * @param layoutId The resource ID of the layout to be inflated for the inventory item view.
     * @return A {@link View} object representing the inventory item's custom layout.
     */
    private View createInventoryItemView(Cursor cursor, int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(layoutId, null);

        String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
        int[] itemQuantity = {cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))};

        TextView itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
        TextView itemQuantityTextView = itemView.findViewById(R.id.itemQuantityTextView);


        itemNameTextView.setText(itemName);
        itemQuantityTextView.setText(String.valueOf(itemQuantity[0]));

        // Add button and logic to increment item quantity
        Button addQtyButton = itemView.findViewById(R.id.itemAddButton);
        addQtyButton.setOnClickListener(v -> {
            int newQuantity = ++itemQuantity[0];

            // Attempt to update item in database. try may not be needed for on-device db
            // but best practice for an external db.
            try {
                inventoryDbHelper.updateItemQuantity(itemName, newQuantity);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error updating item quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            itemQuantityTextView.setText(String.valueOf(newQuantity));
            itemQuantity[0] = newQuantity;
            checkItemQuantity(itemName, newQuantity);
        });

        // Subtract button and logic to decrease item quantity
        Button subQtyButton = itemView.findViewById(R.id.itemSubtractButton);
        subQtyButton.setOnClickListener(v -> {
            if (itemQuantity[0] > 0) {
                int newQuantity = --itemQuantity[0];

                // Attempt to update item in database. try may not be needed for on-device db
                // but best practice for an external db.
                try {
                    inventoryDbHelper.updateItemQuantity(itemName, newQuantity);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error updating item quantity", Toast.LENGTH_SHORT).show();
                    return;
                }

                itemQuantityTextView.setText(String.valueOf(newQuantity));
                itemQuantity[0] = newQuantity;
                checkItemQuantity(itemName, newQuantity);
            }
        });

        // TODO: Setup logic to display itemImage
        // TODO: Swipe left to delete item from inventory

        return itemView;
    }


    /**
     * Populates a specified view group with inventory items retrieved from the database.
     * The method dynamically inflates item views based on the layout type, either list or grid,
     * and adds them to the provided view group. Existing views in the view group
     * are cleared before new views are added.
     *
     * @param view The {@link ViewGroup} where inventory item views will be added.
     *             Must have a tag indicating the layout type (either "item_view_list" or "item_view_grid").
     */
    private void populateListViewWithInventoryItems(ViewGroup view) {
        view.removeAllViews();
        Cursor cursor = inventoryDbHelper.getAllItems();

        int layoutId = R.layout.item_view_list;
        if (view.getTag().equals("item_view_grid")) {
            layoutId = R.layout.item_view_grid;
        }

        // TODO: Header logic removed due to 'new' list view. Setup filters
        if (cursor != null && cursor.moveToFirst()) {
            do {
                view.addView(createInventoryItemView(cursor, layoutId));
            } while (cursor.moveToNext());
        }
        cursor.close();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Item");

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_add_item, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText inputName = viewInflated.findViewById(R.id.input_item_name);
        final EditText inputQuantity = viewInflated.findViewById(R.id.input_item_quantity);

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            if (inputName.getText().toString().isEmpty() || inputQuantity.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please enter item name and quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                dialog.dismiss();
                String itemName = inputName.getText().toString();
                int itemQuantity = Integer.parseInt(inputQuantity.getText().toString());
                inventoryDbHelper.addItem(itemName, itemQuantity);
                ViewGroup view = findViewById(R.id.list_view_layout);
                view.removeAllViews();
                populateListViewWithInventoryItems(view);
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
     * Sends a notification to alert the user that a specified item is out of stock.
     *
     * The notification is created and displayed using the Android NotificationManager. It includes
     * a title, content text indicating the item name, a small icon, and ensures visibility to the user.
     * A notification channel is created for devices running Android O and above to manage notification settings.
     *
     * @param itemName The name of the item that is out of stock. This is displayed in the notification content.
     */
    @SuppressLint("NotificationPermission") // Suppressing as minSDK is 34
    private void sendNotification(String itemName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "inventory_channel";

        NotificationChannel channel = new NotificationChannel(channelId, "Inventory Notifications", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Inventory Alert")
                .setContentText("Item " + itemName + " is out of stock!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);;

        notificationManager.notify(1, builder.build());
    }


    // TODO: Create method to obtain phone number from operator


    /**
     * Sends an SMS message to a specified phone number with a given message content.
     *
     * @param phoneNumber The recipient's phone number to which the SMS will be sent. Must be in a valid format.
     * @param message The message content to be sent.*/
    private void sendSms(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }


    /**
     * Checks the quantity of a specified item and performs actions if the quantity is zero.
     * Sends a notification and SMS message to alert that the item is out of stock.
     *
     * @param itemName The name of the item being checked.
     * @param quantity The current quantity of the item to be evaluated.
     */
    private void checkItemQuantity(String itemName, int quantity) {
        if (quantity == 0) {
            sendNotification(itemName);
            sendSms("1234567890", "Item " + itemName + " is out of stock!");
        }
    }

}