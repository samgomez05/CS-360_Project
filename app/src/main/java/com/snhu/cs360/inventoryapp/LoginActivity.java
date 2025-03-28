package com.snhu.cs360.inventoryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class LoginActivity extends AppCompatActivity {

    //
    // Author:   Samuel Gomez
    // Company:  Southern New Hampshire University
    // Course:   CS-360
    //

    private LoginDatabaseHelper loginDbHelper;
    private EditText usernameField;
    private EditText passwordField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database helper and UI elements
        loginDbHelper = new LoginDatabaseHelper(this);
        usernameField = findViewById(R.id.username_field);
        passwordField = findViewById(R.id.password_field);
        Button loginButton = findViewById(R.id.login_button);

        // Properly set custom toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_login);
        setSupportActionBar(toolbar);

        // Check if the database is empty and insert admin user if it is
        // TODO: Add method to change password on first login
        if (isFirstTimeSetup()) {
            loginDbHelper.addUser("admin", "admin123");
            Toast.makeText(this, "Admin user created with username: admin and password: admin123", Toast.LENGTH_LONG).show();
        }

        // Login button click listener to authenticate user and login
        loginButton.setOnClickListener(v -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();
            if (authenticateUser(username, password)) {
                loginUser();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Determines if the application is being initialized for the first time by checking
     * if the user database contains any records.
     *
     * @return true if there are no user records in the database, indicating a first-time setup;
     *         false otherwise.
     */
    private boolean isFirstTimeSetup() {
        Cursor cursor = loginDbHelper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + LoginDatabaseHelper.TABLE_USERS, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            return count == 0;
        }
        return false;
    }


    /**
     * Authenticates a user by verifying their credentials against the stored password in the database.
     *
     * @param username the username of the user attempting to authenticate
     * @param password the plaintext password provided by the user for authentication
     * @return true if the provided credentials match the stored credentials, false otherwise
     */
    private boolean authenticateUser(String username, String password) {
        Cursor cursor = loginDbHelper.getUser(username);
        if (cursor.moveToFirst()) {
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            return storedPassword.equals(loginDbHelper.hashPassword(password));
        }
        cursor.close();
        return false;
    }


    /**
     * Logs in the user by updating the shared preferences to reflect the user's logged-in status.
     *
     * This method sets a flag in the shared preferences indicating that the user is logged in.
     * The preference is stored under the key "is_logged_in" within the "user_session" shared preferences.
     * Changes are committed asynchronously using the `apply` method.
     */
    private void loginUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }
}