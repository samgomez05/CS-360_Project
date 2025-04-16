package com.snhu.cs360.inventoryapp.firebase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.database.DatabaseReference;
import com.snhu.cs360.inventoryapp.inventory.InventoryItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test class for {@code FirebaseDatabaseHelper}.
 * <p>
 * This class verifies the functionality of database operations including adding, updating, and deleting
 * inventory items via the {@code FirebaseDatabaseHelper} class using mock objects.
 * It ensures proper interactions with the {@code DatabaseReference}.
 */
public class FirebaseDatabaseHelperTest {

    private DatabaseReference databaseReference;
    private FirebaseDatabaseHelper firebaseDatabaseHelper;

    @BeforeEach
    public void setUp() {
        // Setting up db reference and db helper before each test.
        databaseReference = mock(DatabaseReference.class);
        firebaseDatabaseHelper = new FirebaseDatabaseHelper(databaseReference);
    }


    @Test
    public void testAddItem() {
        InventoryItem item = new InventoryItem();
        DatabaseReference pushedRef = mock(DatabaseReference.class);

        // Mock methods
        when(databaseReference.push()).thenReturn(pushedRef);
        when(pushedRef.getKey()).thenReturn("Item1");
        when(databaseReference.child("Item1")).thenReturn(pushedRef);

        // Test adding item
        firebaseDatabaseHelper.addItem(item);
        verify(pushedRef).setValue(item);
    }


    @Test
    public void testUpdateItem() {
        String itemId = "Item1";
        InventoryItem updatedItem = new InventoryItem();

        // Mock item reference and methods
        DatabaseReference itemReference = mock(DatabaseReference.class);
        when(databaseReference.child(itemId)).thenReturn(itemReference);

        // Test update method
        firebaseDatabaseHelper.updateItem(itemId, updatedItem);
        verify(itemReference).setValue(updatedItem);
    }


    @Test
    public void testDeleteItem() {
        String itemId = "deleteMe";

        // Mock item reference
        DatabaseReference itemRef = mock(DatabaseReference.class);
        var mockTask = mock(com.google.android.gms.tasks.Task.class);

        // Mock methods
        when(databaseReference.child(itemId)).thenReturn(itemRef);
        when(itemRef.removeValue()).thenReturn(mockTask);

        // Test delete method
        firebaseDatabaseHelper.deleteItem(itemId);
        verify(itemRef).removeValue();
    }

}