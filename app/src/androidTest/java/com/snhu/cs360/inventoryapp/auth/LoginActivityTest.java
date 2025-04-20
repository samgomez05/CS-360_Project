package com.snhu.cs360.inventoryapp.auth;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.snhu.cs360.inventoryapp.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Instrumented test for LoginActivity class.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {


    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule = new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() { }


    @Test
    public void testLoginUIVisibility() {
        // Check if email field, password field, and login button are displayed
        Espresso.onView(withId(R.id.email_field))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.password_field))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.login_button))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.register_button))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }


    @Test
    public void testLoginWithValidCredentials() {
        FirebaseAuth mockFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        Task<AuthResult> mockTask = Mockito.mock(Task.class);

        // Mock behavior for signInWithEmailAndPassword
        Mockito.when(mockFirebaseAuth.signInWithEmailAndPassword(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mockTask);

        Mockito.when(mockTask.isSuccessful()).thenReturn(true);
        Mockito.when(mockTask.addOnCompleteListener(Mockito.any()))
                .thenAnswer(invocation -> {
                    OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
                    listener.onComplete(mockTask);
                    return mockTask;
                });

        // Launch LoginActivity
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);
        scenario.onActivity(activity -> activity.setFirebaseAuth(mockFirebaseAuth));

        // Input email and password
        Espresso.onView(withId(R.id.email_field))
                .perform(ViewActions.typeText("testuser@example.com"), ViewActions.closeSoftKeyboard());
        Espresso.onView(withId(R.id.password_field))
                .perform(ViewActions.typeText("TestPassword123"), ViewActions.closeSoftKeyboard());

        // Click the login button
        Espresso.onView(withId(R.id.login_button)).perform(ViewActions.click());

        // Verify that login was attempted
        Mockito.verify(mockFirebaseAuth).signInWithEmailAndPassword("testuser@example.com", "TestPassword123");
    }


    @Test
    public void testRegistrationDialogAppearance() {
        FirebaseAuth mockFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        Task<AuthResult> mockTask = Mockito.mock(Task.class);

        // Mock behavior for createUserWithEmailAndPassword
        Mockito.when(mockFirebaseAuth.createUserWithEmailAndPassword(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mockTask);

        Mockito.when(mockTask.isSuccessful()).thenReturn(true);
        Mockito.when(mockTask.addOnCompleteListener(Mockito.any()))
                .thenAnswer(invocation -> {
                    OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
                    listener.onComplete(mockTask); // Trigger callback
                    return mockTask;
                });

        // Launch LoginActivity
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);
        scenario.onActivity(activity -> activity.setFirebaseAuth(mockFirebaseAuth));

        // Open registration dialog
        Espresso.onView(withId(R.id.register_button)).perform(ViewActions.click());

        // Input email and password
        Espresso.onView(withId(R.id.input_user_email))
                .perform(ViewActions.typeText("fakeEmail@example.com"), ViewActions.closeSoftKeyboard());
        Espresso.onView(withId(R.id.input_user_password))
                .perform(ViewActions.typeText("MockPassword123"), ViewActions.closeSoftKeyboard());

        // Confirm dialog is shown
        Espresso.onView(withText(R.string.registerOk))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // Click the register button
        Espresso.onView(withText(R.string.registerOk)).perform(ViewActions.click());

        // Verify interaction
        Mockito.verify(mockFirebaseAuth)
                .createUserWithEmailAndPassword("fakeEmail@example.com", "MockPassword123");
    }

}