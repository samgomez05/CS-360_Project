package com.snhu.cs360.inventoryapp;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class FirebaseDatabaseHelper {


    private final DatabaseReference databaseReference;


    /**
     * Initializes a FirebaseDatabaseHelper instance and connects to the "inventory" node
     * in the Firebase Realtime Database. This node will be used for all database operations
     * such as adding, updating, fetching, and deleting inventory items.
     */
    public FirebaseDatabaseHelper(String referencePath) {
        // Connects to the "referencePath" node in the Firebase database
        databaseReference = FirebaseDatabase.getInstance().getReference(referencePath);
    }


    /**
     * Adds a new inventory item to the Firebase database by generating a unique ID
     * for the item and storing it under the "inventory" node.
     * <p>
     * @param item the inventory item to be added to the database
     */
    public void addItem(InventoryItem item) {
        String itemId = databaseReference.push().getKey(); // Auto-generate unique ID
        if (itemId != null) {
            databaseReference.child(itemId).setValue(item);
        }
    }


    /**
     * Fetches all items from the Firebase database and listens for real-time updates.
     * This method adds a ValueEventListener to the database reference, which triggers
     * the provided listener whenever the data changes.
     * <p>
     * @param listener the ValueEventListener to handle database events and updates
     */
    public void fetchItems(ValueEventListener listener) {
        databaseReference.addValueEventListener(listener);
    }


    /**
     * Updates an existing inventory item in the Firebase database with the specified updated details.
     * <p>
     * @param itemId the unique identifier of the inventory item to be updated
     * @param updatedItem the updated inventory item data to replace the existing item in the database
     */
    public void updateItem(String itemId, InventoryItem updatedItem) {
        databaseReference.child(itemId).setValue(updatedItem);
    }


    /**
     * Deletes an item from the Firebase database identified by its unique ID.
     * <p>
     * @param itemId the unique identifier of the item to be deleted from the database
     */
    public void deleteItem(String itemId) {
        databaseReference.child(itemId).removeValue()
                .addOnFailureListener(Throwable::printStackTrace);
    }

}