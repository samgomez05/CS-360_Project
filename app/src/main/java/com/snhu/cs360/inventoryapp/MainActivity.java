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

        // Get LinearLayout view and populate it with inventory items
        // TODO: Could not setup 'grid' layout and looks as list. Need to fix this.
        LinearLayout linearLayout = findViewById(R.id.list_view_layout);

        populateListViewWithInventoryItems(linearLayout);

        // On FAB press, show dialogue to add item to inventory
        findViewById(R.id.fab_main).setOnClickListener(v -> showAddItemDialog());

        // TODO: Setup menu dialog with options to encompass both logout and layout preference

    }

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

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        switch (String.valueOf(item.getTitle())) {
            case "Logout":
                logout();
                break;
        }

        return super.onOptionsItemSelected(item);
    }




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


    // Verifies if user is logged in by checking shared preferences
    private boolean isUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_logged_in", false);
    }


    // Logout method to clear user session and redirect to login activity
    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private View createInventoryItemView(Cursor cursor) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_view_list, null);

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


    private void populateListViewWithInventoryItems(ViewGroup view) {
        view.removeAllViews();
        Cursor cursor = inventoryDbHelper.getAllItems();

        // TODO: Header logic removed due to 'new' list view. Setup filters
        if (cursor != null && cursor.moveToFirst()) {
            do {
                view.addView(createInventoryItemView(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    // Populates GridLayout with inventory items from SQLite database created in
    // InventoryDatabaseHelper
    // TODO: private void populateGridWithInventoryItems(View view) {}


    // On FAB press, show dialogue to add item to inventory
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
                LinearLayout view = findViewById(R.id.list_view_layout);
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


    // Application Notification method to alert user if an item is out of stock
    // and send SMS to a hardcoded phone number (temporary) as well as display a notification
    // on the device.
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
    private void sendSms(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }


    private void checkItemQuantity(String itemName, int quantity) {
        if (quantity == 0) {
            sendNotification(itemName);
            sendSms("1234567890", "Item " + itemName + " is out of stock!");
        }
    }

}