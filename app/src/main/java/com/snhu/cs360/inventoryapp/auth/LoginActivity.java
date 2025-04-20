package com.snhu.cs360.inventoryapp.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snhu.cs360.inventoryapp.MainActivity;
import com.snhu.cs360.inventoryapp.R;


public class LoginActivity extends AppCompatActivity {

    /*
     *
     * Author:   Samuel Gomez
     * Company:  Southern New Hampshire University
     * Course:   CS-360
     *
     */

    private EditText emailField;
    private EditText passwordField;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Properly set custom toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_login);
        setSupportActionBar(toolbar);

        // Initialize database helper and UI elements
        firebaseAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.email_field);
        passwordField = findViewById(R.id.password_field);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v -> showRegistrationDialog());
        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            loginUser(email, password);
        });
    }

    @VisibleForTesting
    public void setFirebaseAuth(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    /**
     * Attempts to log in a user using the provided email and password credentials.
     * If the login is successful, it retrieves the current Firebase user and initiates the login process.
     * If the login fails, it displays an appropriate error message.
     *
     * @param email    The email address of the user attempting to log in.
     * @param password The password associated with the provided email address.
     */
    private void loginUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    login();
                }
            } else {
                Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Completes the user login process by setting a preference indicating a successful login
     * and initiating the MainActivity.
     * <p>
     * This method performs the following actions: <p>
     * 1. Updates the shared preferences to mark the user as logged in. <p>
     * 2. Starts the MainActivity to proceed with the user session. <p>
     * 3. Finalizes the LoginActivity to prevent navigation back to it. <p>
     * <p>
     * It assumes that the authentication process has already succeeded before this method is invoked.
     */
    private void login() {
        // Set preference entry as successful
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true);
        editor.apply();

        // Authentication successful, start MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * Registers a new user using the provided email and password credentials. <p>
     * If the registration is successful, the user is notified with a success message.
     * If the registration fails, the user is informed with an error message detailing the failure reason.
     *
     * @param email    The email address to be registered for the new user.
     * @param password The password to be associated with the provided email address.
     */
    private void registerUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Registration successful. Please log in.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Displays a registration dialog to prompt the user for email and password input.
     * <p>
     * This method creates and shows an AlertDialog configured with a custom view containing
     * input fields for email and password. The user can either confirm their input to
     * initiate the registration process or cancel the dialog.
     * <p>
     * The following validations are performed on user input: <p>
     * - Email and password fields should not be empty. <p>
     * - Email must follow a valid email format. <p>
     * - Password must be at least 8 characters long. <p>
     * <p>
     * If the input passes validation, the registration process is initiated by invoking
     * the registerUser method. Appropriate Toast messages are displayed for invalid input
     * or successful/failed registration.
     */
    private void showRegistrationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Registration Form");

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_register_user, findViewById(android.R.id.content), false);
        final EditText inputEmail = viewInflated.findViewById(R.id.input_user_email);
        final EditText inputPassword = viewInflated.findViewById(R.id.input_user_password);

        builder.setView(viewInflated);
        builder.setPositiveButton(R.string.registerOk, (dialog, which) -> {

            // Email and password validations
            if (inputEmail.getText().toString().isEmpty() || inputPassword.getText().toString().isEmpty() ) {
                Toast.makeText(this, "Please enter email, and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString()).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputPassword.getText().toString().length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(inputEmail.getText().toString(), inputPassword.getText().toString());

        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }
}