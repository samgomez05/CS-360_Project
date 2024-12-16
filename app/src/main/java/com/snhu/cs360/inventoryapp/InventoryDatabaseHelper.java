package com.snhu.cs360.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class InventoryDatabaseHelper extends SQLiteOpenHelper {
    //
    // Author:   Samuel Gomez
    // Company:  Southern New Hampshire University
    // Course:   CS-360
    //

    // TODO: MAJOR REWORK
    //       Implement methods to manage a shared database for multiple users and locations
    //         - Possibly an API for cloud database

    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_INVENTORY = "inventory";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ITEM_NAME = "item_name";
    private static final String COLUMN_QUANTITY = "quantity";


    public InventoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    // Creates the inventory table
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
                COLUMN_QUANTITY + " INTEGER NOT NULL)";
        db.execSQL(CREATE_TABLE);
        if (isTableEmpty(db)) {
            insertSampleItems(db);
        }
    }


    // Drop the inventory table if it exists and create a new one
    // TODO: Not entirely sure, but likely needs to be updated to maintain
    //       existing data when upgrading the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }


    // Adds a new item to the inventory table
    public void addItem(String itemName, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, itemName);
        values.put(COLUMN_QUANTITY, quantity);
        db.insert(TABLE_INVENTORY, null, values);
        db.close();
    }


    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_INVENTORY;
        return db.rawQuery(query, null);
    }


    // Check if the inventory table is empty and insert sample items if it is
    private boolean isTableEmpty(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_INVENTORY, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            return count == 0;
        }
        return false;
    }


    // Insert sample items into the inventory table
    // TODO: Method needs updating to suggest to user if they want to insert sample items
    private void insertSampleItems(SQLiteDatabase db) {
        for (int i = 1; i <= 20; i++) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ITEM_NAME, "Item " + i);
            values.put(COLUMN_QUANTITY, i * 10);
            db.insert(TABLE_INVENTORY, null, values);
        }
    }


    // Update the quantity of an item in the inventory table
    public void updateItemQuantity(String itemName, int newQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUANTITY, newQuantity);
        db.update(TABLE_INVENTORY, values, COLUMN_ITEM_NAME + " = ?", new String[]{itemName});
        db.close();
    }
}