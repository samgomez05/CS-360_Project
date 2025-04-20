package com.snhu.cs360.inventoryapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.snhu.cs360.inventoryapp.firebase.FirebaseDatabaseHelper;
import com.snhu.cs360.inventoryapp.inventory.InventoryAdapter;
import com.snhu.cs360.inventoryapp.inventory.InventoryItem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private FirebaseDatabaseHelper mockDbHelper;

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.POST_NOTIFICATIONS
    );

    @Before
    public void setUp() {
        mockDbHelper = Mockito.mock(FirebaseDatabaseHelper.class);
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("is_logged_in", true).apply();
    }

    @Test
    public void testLaunchAndFabOpensAddDialog() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Simulate FAB click to show add item dialog
        onView(withId(R.id.fab_main)).perform(click());

        // Confirm dialog inputs are visible
        onView(withId(R.id.input_item_name)).check(matches(isDisplayed()));
        onView(withId(R.id.input_item_quantity)).check(matches(isDisplayed()));
    }

    @Test
    public void testSwitchLayoutTogglesView() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.recyclerView);

            // Initial state: should be LinearLayoutManager
            assertTrue(recyclerView.getLayoutManager() instanceof androidx.recyclerview.widget.LinearLayoutManager);

            activity.toggleLayoutManager();

            // After toggle: should be GridLayoutManager
            assertTrue(recyclerView.getLayoutManager() instanceof androidx.recyclerview.widget.GridLayoutManager);
        });
    }

    @Test
    public void testSwipeToDeleteTriggersDialog() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        scenario.onActivity(activity -> {
            InventoryItem testItem = new InventoryItem("1", "Item", "Desc", 1, "Tag");
            ArrayList<InventoryItem> items = new ArrayList<>();
            items.add(testItem);

            activity.inventoryAdapter = new InventoryAdapter(items, true, mockDbHelper);
            activity.recyclerView.setAdapter(activity.inventoryAdapter);
        });

        // Use built-in Espresso swipe
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeLeft()));

        // Optionally verify the delete dialog shows
        onView(withText("Delete Item")).check(matches(isDisplayed()));
    }

}
