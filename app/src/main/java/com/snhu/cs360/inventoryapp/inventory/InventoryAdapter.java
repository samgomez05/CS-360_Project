package com.snhu.cs360.inventoryapp.inventory;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.snhu.cs360.inventoryapp.firebase.FirebaseDatabaseHelper;
import com.snhu.cs360.inventoryapp.R;

import java.util.List;


/**
 * Custom adapter class for managing and displaying a list of inventory items in a RecyclerView.
 * It supports switching between list and grid view modes and interacts with a Firebase
 * database to update inventory item data in real-time.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private boolean isListView;
    private List<InventoryItem> mInventoryList;
    private FirebaseDatabaseHelper mFirebaseDatabaseHelper;


    public InventoryAdapter(List<InventoryItem> inventoryList, boolean isListView, FirebaseDatabaseHelper firebaseDatabaseHelper) {
        mInventoryList = inventoryList;
        this.isListView = isListView;
        this.mFirebaseDatabaseHelper = firebaseDatabaseHelper;
    }


    /**
     * ViewHolder subclass for the InventoryAdapter, used to hold and manage the Views that represent
     * an individual item in the Inventory list within the RecyclerView.
     * <p>
     * This class provides member variables for accessing TextViews and Buttons associated with
     * an inventory item, such as item name, description, quantity, and actions for increasing or
     * decreasing the item quantity.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView itemNameTextView;
        public TextView itemDescriptionTextView;
        public TextView itemQuantityTextView;
        public Button itemAddButton;
        public Button itemSubtractButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
            itemDescriptionTextView = itemView.findViewById(R.id.itemDescriptionTextView);
            itemQuantityTextView = itemView.findViewById(R.id.itemQuantityTextView);
            itemAddButton = itemView.findViewById(R.id.itemAddButton);
            itemSubtractButton = itemView.findViewById(R.id.itemSubtractButton);

        }
    }


    /**
     * Creates and returns a new ViewHolder for representing an inventory item in the RecyclerView.
     * This method inflates the layout based on the current view mode (list or grid).
     * <p>
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View. This parameter is not used directly in this method.
     * @return A new instance of InventoryAdapter.ViewHolder for holding the inflated inventory item View.
     */
    @NonNull
    @Override
    public InventoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        int layoutId = isListView ? R.layout.item_view_list : R.layout.item_view_grid;
        View inventoryItemView = inflater.inflate(layoutId, parent, false);

        return new ViewHolder(inventoryItemView);
    }


    /**
     * Binds the data from the inventory item list to the respective views in the ViewHolder.
     * This method is called by the RecyclerView to display the data at the specified position.
     * It also handles increment and decrement actions for the item's quantity, updating both the
     * local item list and the Firebase database, and refreshing the item view.
     * <p>
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *               inventory item at the given position in the dataset.
     * @param position The position of the inventory item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull InventoryAdapter.ViewHolder holder, int position) {
        InventoryItem currentItem = mInventoryList.get(position);

        holder.itemNameTextView.setText(currentItem.getName());
        holder.itemDescriptionTextView.setText(currentItem.getDescription());
        holder.itemQuantityTextView.setText(String.valueOf(currentItem.getQuantity()));

        holder.itemAddButton.setOnClickListener(v -> {
            currentItem.setQuantity(currentItem.getQuantity() + 1);
            mFirebaseDatabaseHelper.updateItem(currentItem.getId(), currentItem);
            notifyItemChanged(position);
        });

        holder.itemSubtractButton.setOnClickListener(v -> {
            if (currentItem.getQuantity() > 0) {
                currentItem.setQuantity(currentItem.getQuantity() - 1);
                mFirebaseDatabaseHelper.updateItem(currentItem.getId(), currentItem);
                notifyItemChanged(position);
            }
        });

        holder.itemView.setOnClickListener(v -> showEditItemDialog(currentItem, position, holder));

    }


    /**
     * Returns the total number of items in the inventory list.
     * <p>
     * @return The size of the inventory list, representing the total number of items.
     */
    @Override
    public int getItemCount() {
        return mInventoryList.size();
    }


    /**
     * Updates the list of inventory items in the adapter and refreshes the data displayed by the RecyclerView.
     *
     * @param items The new list of {@link InventoryItem} objects to be displayed in the inventory.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<InventoryItem> items) {
        this.mInventoryList = items;
        notifyDataSetChanged();
    }


    /**
     * Displays a dialog for editing an existing inventory item's details.
     * The dialog allows updating the item's name, description, quantity, and tag.
     * It also updates the inventory list and synchronizes changes with the Firebase database
     * upon saving.
     * <p>
     * @param currentItem the {@link InventoryItem} representing the inventory item to be edited.
     *                    Pre-fills the dialog inputs with the current details of the item.
     * @param position the position of the {@code currentItem} in the inventory list. Used to update
     *                 the item within the adapter and notify the changes to the RecyclerView.
     * @param holder the {@code ViewHolder} containing the context and item view associated with
     *               the inventory list. Used for inflating the dialog and accessing resources.
     */
    private void showEditItemDialog(InventoryItem currentItem, int position, ViewHolder holder) {
        View dialogView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_edit_view, null);

        // Create the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(holder.itemView.getContext());
        dialogBuilder.setTitle("Edit Item");
        dialogBuilder.setView(dialogView);

        AlertDialog dialog = dialogBuilder.create();

        // Get references to dialog inputs
        EditText editName = dialogView.findViewById(R.id.itemNameEditText);
        EditText editDescription = dialogView.findViewById(R.id.itemDescriptionEditText);
        EditText editQuantity = dialogView.findViewById(R.id.itemQuantityEditText);
        EditText editTag = dialogView.findViewById(R.id.itemTagEditText);

        // Pre-fill dialog inputs with current item's data
        editName.setText(currentItem.getName());
        editDescription.setText(currentItem.getDescription());
        editQuantity.setText(String.valueOf(currentItem.getQuantity()));
        editTag.setText(currentItem.getTag());

        // Set up confirm and cancel buttons
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        saveButton.setOnClickListener(v -> {

            // Update the inventory item with new data
            currentItem.setName(editName.getText().toString());
            currentItem.setDescription(editDescription.getText().toString());
            currentItem.setQuantity(Integer.parseInt(editQuantity.getText().toString()));
            currentItem.setTag(editTag.getTag().toString());

            // Update the adapter and notify changes
            mInventoryList.set(position, currentItem);
            notifyItemChanged(position);

            // Update the item in Firebase database
            mFirebaseDatabaseHelper.updateItem(currentItem.getId(), currentItem);

            dialog.dismiss(); // Close the dialog
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss()); // Close the dialog on cancel

        dialog.show(); // Display the dialog
    }


    /**
     * Retrieves the inventory item at the specified position in the list.
     * <p>
     * @param position The position of the inventory item in the list.
     * @return The {@code InventoryItem} object located at the specified position.
     */
    public InventoryItem getItemAt(int position) {
        return mInventoryList.get(position);
    }


    /**
     * Removes the specified item from the inventory list and notifies the adapter of the updates.
     * This method also triggers visual updates in the associated RecyclerView to reflect the changes.
     * <p>
     * @param item The {@link InventoryItem} to be removed from the inventory list.
     */
    public void removeItem(InventoryItem item) {
        int index = mInventoryList.indexOf(item);
        if (index >= 0) {
            mInventoryList.remove(index);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, mInventoryList.size());
        }
    }

}
