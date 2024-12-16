package com.snhu.cs360.inventoryapp;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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

        // Hide the action bar and programmatically set title
        getSupportActionBar().hide();
        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.app_name);

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

        // Get GridLayout view and populate it with inventory items
        // TODO: Could not setup 'grid' layout and looks as list. Need to fix this.
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        populateGridWithInventoryItems(gridLayout);

        // On FAB press, show dialogue to add item to inventory
        findViewById(R.id.fab_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddItemDialog();
            }
        });

        // On logout button press, clear user session and redirect to login activity
        ImageButton logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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


    // BULK OF LOGIC FOR MAIN ACTIVITY
    // Populates GridLayout with inventory items from SQLite database created in
    // InventoryDatabaseHelper
    // TODO: Swipe left to delete item from inventory
    private void populateGridWithInventoryItems(GridLayout gridLayout) {
        gridLayout.removeAllViews(); // Clearing out existing views

        // Adding column headers for operator convenience
        String[] headers = {"Item Name", "Quantity", "Add", "Subtract"};
        for (String header : headers) {
            TextView headerView = new TextView(this);
            headerView.setText(header);
            headerView.setTypeface(null, Typeface.BOLD);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            headerView.setLayoutParams(params);
            gridLayout.addView(headerView);
        }

        Cursor cursor = inventoryDbHelper.getAllItems();
        if (cursor != null && cursor.moveToFirst()) {
            // Number of columns in the grid (item name, quantity, add button, subtract button)
            int columnCount = 4;
            gridLayout.setColumnCount(columnCount);

            do {
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                int[] itemQuantity = {cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))};

                TextView itemNameView = new TextView(this);
                itemNameView.setText(itemName);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                itemNameView.setLayoutParams(params);

                TextView itemQuantityView = new TextView(this);
                itemQuantityView.setText(String.valueOf(itemQuantity[0]));
                params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                itemQuantityView.setLayoutParams(params);

                // Add button and logic to increment item quantity
                Button addButton = new Button(this);
                addButton.setText("+");
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int newQuantity = itemQuantity[0] + 1;
                        inventoryDbHelper.updateItemQuantity(itemName, newQuantity);
                        itemQuantityView.setText(String.valueOf(newQuantity));
                        itemQuantity[0] = newQuantity;
                    }
                });
                params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                addButton.setLayoutParams(params);

                // Subtract button and logic to decrement item quantity
                Button subtractButton = new Button(this);
                subtractButton.setText("-");
                subtractButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (itemQuantity[0] > 0) {
                            int newQuantity = itemQuantity[0] - 1;
                            inventoryDbHelper.updateItemQuantity(itemName, newQuantity);
                            itemQuantityView.setText(String.valueOf(newQuantity));
                            itemQuantity[0] = newQuantity;
                            checkItemQuantity(itemName, newQuantity);
                        }
                    }
                });
                params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                subtractButton.setLayoutParams(params);

                gridLayout.addView(itemNameView);
                gridLayout.addView(itemQuantityView);
                gridLayout.addView(addButton);
                gridLayout.addView(subtractButton);
            } while (cursor.moveToNext()); // For each item in the inventory

            cursor.close();
        }
    }


    // On FAB press, show dialogue to add item to inventory
    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Item");

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_add_item, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText inputName = viewInflated.findViewById(R.id.input_item_name);
        final EditText inputQuantity = viewInflated.findViewById(R.id.input_item_quantity);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String itemName = inputName.getText().toString();
                int itemQuantity = Integer.parseInt(inputQuantity.getText().toString());
                inventoryDbHelper.addItem(itemName, itemQuantity);
                GridLayout gridLayout = findViewById(R.id.grid_layout);
                gridLayout.removeAllViews();
                populateGridWithInventoryItems(gridLayout);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

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