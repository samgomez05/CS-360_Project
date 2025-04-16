package com.snhu.cs360.inventoryapp.inventory;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snhu.cs360.inventoryapp.R;
import com.snhu.cs360.inventoryapp.firebase.FirebaseDatabaseHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InventoryAdapterTest {

    private Context testContext;
    private InventoryAdapter adapter;
    private List<InventoryItem> mockInventoryList;

    @Before
    public void setUp() {
        // Initialize the test Context
        testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Setting up a mock inventory list using non-empty constructor of InventoryItem
        mockInventoryList = new ArrayList<>();
        mockInventoryList.add(new InventoryItem("Item1", "Test Item1", "Some Description", 5, "Tag1"));
        mockInventoryList.add(new InventoryItem("Item2","Test Item2", "Some Description", 10, "Tag2"));
        mockInventoryList.add(new InventoryItem("Item3", "Test Item3", "Some Description", 15, "Tag3"));

        FirebaseDatabaseHelper mockFirebaseDatabaseHelper = mock(FirebaseDatabaseHelper.class);
        doNothing().when(mockFirebaseDatabaseHelper).updateItem(anyString(), any(InventoryItem.class));

        adapter = new InventoryAdapter(mockInventoryList, true, mockFirebaseDatabaseHelper);

    }


    @Test
    public void testGetItemCount() {
        // Verify that the adapter's item count matches the size of the mock data
        int itemCount = adapter.getItemCount();
        assertThat(itemCount, is(mockInventoryList.size()));
    }

    @Test
    public void testOnBindViewHolderList() {
        // Create a mock ViewHolder
        LayoutInflater layoutInflater = LayoutInflater.from(testContext);
        ViewGroup mockParent = (ViewGroup) layoutInflater.inflate(R.layout.item_view_list, null);
        InventoryAdapter.ViewHolder mockViewHolder = adapter.onCreateViewHolder(mockParent, 0);

        // Bind a specific item from the mock list
        adapter.onBindViewHolder(mockViewHolder, 0);

        // Verify the UI components within the ViewHolder reflect the bound data
        InventoryItem boundItem = mockInventoryList.get(0);
        boundItem.setId(boundItem.getName());
        assertThat(mockViewHolder.itemNameTextView.getText().toString(), is(boundItem.getName()));
        assertThat(mockViewHolder.itemQuantityTextView.getText().toString(), is(String.valueOf(boundItem.getQuantity())));
    }

    @Test
    public void testOnBindViewHolderGrid() {
        // Create a mock ViewHolder
        LayoutInflater layoutInflater = LayoutInflater.from(testContext);
        ViewGroup mockParent = (ViewGroup) layoutInflater.inflate(R.layout.item_view_grid, null);
        InventoryAdapter.ViewHolder mockViewHolder = adapter.onCreateViewHolder(mockParent, 0);

        // Bind a specific item from the mock list
        adapter.onBindViewHolder(mockViewHolder, 0);

        // Verify the UI components within the ViewHolder reflect the bound data
        InventoryItem boundItem = mockInventoryList.get(0);
        boundItem.setId("");
        assertThat(mockViewHolder.itemNameTextView.getText().toString(), is(boundItem.getName()));
        assertThat(mockViewHolder.itemQuantityTextView.getText().toString(), is(String.valueOf(boundItem.getQuantity())));
    }

    @Test
    public void testIncrementAndDecrementQuantityList() {
        // Bind the first item to a mock ViewHolder
        LayoutInflater layoutInflater = LayoutInflater.from(testContext);
        ViewGroup mockParent = (ViewGroup) layoutInflater.inflate(R.layout.item_view_list, null);
        InventoryAdapter.ViewHolder mockViewHolder = adapter.onCreateViewHolder(mockParent, 0);

        adapter.onBindViewHolder(mockViewHolder, 0);

        // Simulate increment button click
        mockViewHolder.itemAddButton.performClick();
        assertThat(mockInventoryList.get(0).getQuantity(), is(6)); // Quantity should increment by 1

        // Simulate decrement button click
        mockViewHolder.itemSubtractButton.performClick();
        assertThat(mockInventoryList.get(0).getQuantity(), is(5)); // Quantity should decrement by 1
    }

    @Test
    public void testIncrementAndDecrementQuantityGrid() {
        // Bind the first item to a mock ViewHolder
        LayoutInflater layoutInflater = LayoutInflater.from(testContext);
        ViewGroup mockParent = (ViewGroup) layoutInflater.inflate(R.layout.item_view_grid, null);
        InventoryAdapter.ViewHolder mockViewHolder = adapter.onCreateViewHolder(mockParent, 0);

        adapter.onBindViewHolder(mockViewHolder, 0);

        // Simulate increment button click
        mockViewHolder.itemAddButton.performClick();
        assertThat(mockInventoryList.get(0).getQuantity(), is(6)); // Quantity should increment by 1

        // Simulate decrement button click
        mockViewHolder.itemSubtractButton.performClick();
        assertThat(mockInventoryList.get(0).getQuantity(), is(5)); // Quantity should decrement by 1
    }
}