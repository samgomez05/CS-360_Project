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
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ITEM_NAME = "item_name";
    private static final String COLUMN_QUANTITY = "item_quantity";
    private static final String COLUMN_DESCRIPTION = "item_description";


    public InventoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /**
     * Called when the database is created for the first time.
     * This method creates the inventory table and inserts sample items
     * if the table is initially empty.
     *
     * @param db The SQLite database instance to be initialized.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_QUANTITY + " INTEGER NOT NULL)";
        db.execSQL(CREATE_TABLE);
        if (isTableEmpty(db)) {
            insertSampleItems(db);
        }
    }


    /**
     * Called when the database needs to be upgraded. This method is triggered when the database
     * version is incremented. It drops the existing inventory table and recreates it
     * by calling the onCreate method, ensuring the database structure and initial data
     * are updated to the new version.
     *
     * @param db         The SQLite database instance to be upgraded.
     * @param oldVersion The old version number of the database.
     * @param newVersion The new version number of the database.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: Not entirely sure, but likely needs to be updated to maintain
        //       existing data when upgrading the database
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }


    /**
     * Retrieves all items from the inventory table in the database.
     *
     * @return A Cursor object containing the result set of items from the inventory table.
     */
    public Cursor getAllInventoryItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("inventory", null, null, null,
                null, null, null);
    }


    /**
     * Searches the inventory table for items matching the given search query.
     * This method performs a query on the "item_name" column using a SQL LIKE statement
     * based on the provided search query.
     *
     * @param searchQuery The query string used to search for items in the inventory table.
     *                    The search is case-insensitive and allows partial matches.
     * @return A Cursor object containing the result set of items that match the search query.
     *         If no items match, the Cursor will be empty but not null.
     */
    public Cursor searchInventoryItems(String searchQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("inventory", null, "item_name LIKE ?", new String[] {"%" + searchQuery + "%"},
                null, null, null);
    }


    /**
     * Inserts a new item into the inventory table in the database.
     *
     * @param itemName The name of the item to be added to the inventory.
     * @param quantity The quantity of the item to be added to the inventory.
     */
    public void addItem(String itemName, String itemDescription, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, itemName);
        values.put(COLUMN_QUANTITY, quantity);
        values.put(COLUMN_DESCRIPTION, itemDescription);
        try {
            db.insert(TABLE_INVENTORY, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }


    /**
     * Checks if the inventory table in the database is empty.
     *
     * @param db The SQLite database instance to be checked.
     * @return true if the inventory table is empty; false otherwise.
     */
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


    /**
     * Inserts sample items into the inventory table in the database.
     *
     * @param db The SQLite database instance where the sample items will be inserted.
     */
    private void insertSampleItems(SQLiteDatabase db) {
        // TODO: Method needs updating to suggest to user if they want to insert sample items
        try {
            for (int i = 1; i <= 20; i++) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ITEM_NAME, "Item " + i);
                values.put(COLUMN_DESCRIPTION, "Description of Item " + i);
                values.put(COLUMN_QUANTITY, i * 10);
                db.insert(TABLE_INVENTORY, null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Updates the quantity of a specific item in the inventory table in the database.
     *
     * @param itemName   The name of the item whose quantity is to be updated.
     * @param newQuantity The new quantity to update for the specified item.
     */
    public void updateItemQuantity(String itemName, int newQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUANTITY, newQuantity);
        try {
            db.update(TABLE_INVENTORY, values, COLUMN_ITEM_NAME + " = ?", new String[]{itemName});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}