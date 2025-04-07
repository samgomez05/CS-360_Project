package com.snhu.cs360.inventoryapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


/**
 * Custom adapter class for managing and displaying a list of inventory items in a RecyclerView.
 * It supports switching between list and grid view modes and interacts with a Firebase
 * database to update inventory item data in real-time.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryItem> mInventoryList;
    private FirebaseDatabaseHelper mFirebaseDatabaseHelper;
    private boolean isListView;

    public InventoryAdapter(List<InventoryItem> inventoryList, boolean isListView) {
        mInventoryList = inventoryList;
        this.isListView = isListView;
        mFirebaseDatabaseHelper = new FirebaseDatabaseHelper("inventory");
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
            if (currentItem.getQuantity() >= 0) {
                currentItem.setQuantity(currentItem.getQuantity() - 1);
                mFirebaseDatabaseHelper.updateItem(currentItem.getId(), currentItem);
                notifyItemChanged(position);
            }
        });

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

}
