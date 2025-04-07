package com.snhu.cs360.inventoryapp;


/**
 * Represents an item in an inventory system.
 * This class encapsulates the details of an inventory item such as its identifier, name, description,
 * quantity, and associated tag, and provides methods to access and modify these details.
 * <p>
 * The class is designed to be comparable based on the name of the inventory item to facilitate
 * sorting operations. It also includes a default constructor necessary for use cases like serialization.
 */
public class InventoryItem implements Comparable<InventoryItem> {

    private String id;
    private String name;
    private String description;
    private int quantity;
    private String tag;


    /**
     * Default constructor for InventoryItem.
     * This empty constructor is required for specific use cases, such as serialization or deserialization
     * in frameworks like Firebase, where a no-argument constructor is mandatory.
     */
    public InventoryItem() {

    }

    /**
     * Constructs a new InventoryItem with the specified details.
     * <p>
     * @param name the name of the inventory item
     * @param description a brief description of the inventory item
     * @param quantity the quantity of the inventory item in stock
     * @param tag the tag to which the inventory item belongs
     */
    public InventoryItem(String name, String description, int quantity, String tag) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.tag = tag;
    }


    /*
     *  Getters
     */
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public String getTag() { return tag; }


    /*
     *  Setters
     */
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTag(String tag) { this.tag = tag; }

    /**
     * Compares this InventoryItem with the specified InventoryItem for order based on the name of the inventory item.
     * The comparison is case-insensitive.
     * <p>
     * @param o the InventoryItem to be compared with this InventoryItem.
     * @return a negative integer, zero, or a positive integer as this InventoryItem's name
     *         is lexicographically less than, equal to, or greater than the specified InventoryItem's name.
     */
    @Override
    public int compareTo(InventoryItem o) { return this.name.compareToIgnoreCase(o.name); }
}