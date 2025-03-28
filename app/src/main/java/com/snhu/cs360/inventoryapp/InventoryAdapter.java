package com.snhu.cs360.inventoryapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;


public class InventoryAdapter extends SimpleCursorAdapter {

    private final InventoryDatabaseHelper dbHelper;
    private final LayoutInflater inflater;

    public InventoryAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, InventoryDatabaseHelper dbHelper) {
        super(context, layout, c, from, to, flags);
        this.dbHelper = dbHelper;
        this.inflater = LayoutInflater.from(context);
    }


    /**
     * Binds data from the provided Cursor to the given View for display purposes. This includes setting
     * text values for specific TextViews and attaching onClickListener events to Buttons for updating item
     * quantities in the database.
     *
     * @param view   The view into which the data should be bound, typically associated with a list item.
     * @param context The context in which the method is operating, used for accessing resources and sending notifications.
     * @param cursor  The Cursor containing the data to be displayed. It is positioned at the correct row.
     */
    @Override
    public void bindView(android.view.View view, Context context, Cursor cursor) {
        TextView itemNameTextView = view.findViewById(R.id.itemNameTextView);
        String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
        itemNameTextView.setText(itemName);

        TextView itemDescriptionTextView = view.findViewById(R.id.itemDescriptionTextView);
        String itemDescription = cursor.getString(cursor.getColumnIndexOrThrow("item_description"));
        itemDescriptionTextView.setText(itemDescription);

        TextView itemQuantityTextView = view.findViewById(R.id.itemQuantityTextView);
        int itemQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("item_quantity"));
        itemQuantityTextView.setText(String.valueOf(itemQuantity));

        Button itemAddButton = view.findViewById(R.id.itemAddButton);
        itemAddButton.setOnClickListener(v -> {
            int newQuantity = itemQuantity + 1;
            dbHelper.updateItemQuantity(itemName, newQuantity);
            refreshCursor();
        });

        Button itemSubtractButton = view.findViewById(R.id.itemSubtractButton);
        itemSubtractButton.setOnClickListener(v -> {
            int newQuantity = itemQuantity - 1;
            if (newQuantity >= 0) {
                dbHelper.updateItemQuantity(itemName, newQuantity);
                refreshCursor();

                if (newQuantity == 0) {
                    sendNotification(context, "Inventory Update", itemName +
                            " has run out of stock!");
                    sendSms("+12024561111", "Hi, " + itemName + " has run out of stock!");
                }
            }
        });
    }


    /**
     * Refreshes the Cursor used by the adapter by retrieving the latest set of inventory items
     * from the database and updating the adapter with the new Cursor.
     *
     * This method fetches the most up-to-date data from the database using the dbHelper's
     * getAllInventoryItems method and calls changeCursor to update the current Cursor
     * used in the adapter.
     *
     * This ensures that the adapter reflects any changes made to the inventory data in the database.
     */
    private void refreshCursor() {
        Cursor cursor = dbHelper.getAllInventoryItems();
        changeCursor(cursor);

    }


    /**
     * Replaces the current Cursor with a new Cursor provided as a parameter.
     * The existing Cursor is closed to release resources before assigning the new one.
     *
     * @param cursor The new Cursor object to be used by the adapter.
     *               It holds the data that will be displayed within the adapter.
     */
    @Override
    public void changeCursor(Cursor cursor) {
        if(getCursor() != null) {
            getCursor().close();
        }
        super.changeCursor(cursor);
    }


    /**
     * Sends a notification to the user with a specified title and message.
     *
     * This method is responsible for creating a notification channel if it does not already exist,
     * building a notification using the provided title and message, and displaying it using the NotificationManager.
     * The notification runs with high priority, and is automatically cancelled when clicked.
     *
     * @param context The context in which the notification is created and displayed.
     *                It is used to access system services such as the NotificationManager.
     * @param title The title of the notification to be displayed to the user.
     * @param message The content/message body of the notification to be displayed to the user.
     */
    private void sendNotification(Context context, String title, String message) {
        String channelId = "inventory_channel";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(channelId, "Inventory Notifications", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Notifications for inventory updates");
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);

        // Show the notification
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build()); // 1 is the notification ID
        }
    }



    /**
     * Sends an SMS message to a specified phone number with a given message content.
     *
     * @param phoneNumber The recipient's phone number to which the SMS will be sent. Must be in a valid format.
     * @param message The message content to be sent.*/
    private void sendSms(String phoneNumber, String message) {
        // TODO: Create method to obtain phone number from operator
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

}
