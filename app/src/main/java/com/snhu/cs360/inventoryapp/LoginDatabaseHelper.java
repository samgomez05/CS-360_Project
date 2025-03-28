package com.snhu.cs360.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginDatabaseHelper extends SQLiteOpenHelper {

    //
    // Author:   Samuel Gomez
    // Company:  Southern New Hampshire University
    // Course:   CS-360
    //

    private static final String DATABASE_NAME = "login.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";


    public LoginDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /**
     * Creates the users table in the database if it does not already exist.
     *
     * @param db The SQLite database instance where the table will be created.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT NOT NULL, " +
                COLUMN_PASSWORD + " TEXT NOT NULL)";
        db.execSQL(CREATE_TABLE);
    }


    /**
     * Handles database upgrades when the schema version changes.
     * This method drops the existing users table if it exists
     * and recreates it by calling {@link #onCreate(SQLiteDatabase)}.
     *
     * @param db         The SQLite database instance being upgraded.
     * @param oldVersion The current version of the database.
     * @param newVersion The new version of the database to upgrade to.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }


    /**
     * Adds a new user to the database. The user's password is securely hashed
     * before being stored.
     *
     * @param username The username of the new user to be added.
     * @param password The plaintext password of the new user to be securely hashed and stored.
     */
    public void addUser(String username, String password) {
        // TODO: Method to initialize a new user, not just the default 'admin'
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, hashPassword(password));
        db.insert(TABLE_USERS, null, values);
        db.close();
    }


    /**
     * Retrieves a user record from the database based on the provided username.
     *
     * @param username The username of the user to be retrieved.
     * @return A Cursor object containing the user record that matches the provided username.
     */
    public Cursor getUser(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ?";
        return db.rawQuery(query, new String[]{username});
    }


    /**
     * Hashes a plaintext password using the SHA-256 algorithm to ensure secure storage.
     * The resulting hash is returned as a hexadecimal string.
     *
     * @param password The plaintext password to be hashed.
     * @return The SHA-256 hash of the given password as a hexadecimal string.
     * @throws RuntimeException If the SHA-256 algorithm is not supported on the platform.
     */
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}