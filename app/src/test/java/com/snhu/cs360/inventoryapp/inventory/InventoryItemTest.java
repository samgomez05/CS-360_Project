package com.snhu.cs360.inventoryapp.inventory;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * Unit test class for testing the functionality of the InventoryItem class.
 * <p>
 * This class tests various methods and scenarios related to the InventoryItem class, including:
 * - Verifying the parameterized constructor <p>
 * - Testing the compareTo method for equality, order, and sorting <p>
 * - Checking the correct handling of lexicographical comparisons <p>
 * - Ensuring proper sorting behavior in scenarios involving multiple items <p>
 * <p>
 * The test class uses JUnit annotations and assertions to validate the expected behavior of the InventoryItem class.
 */
public class InventoryItemTest {

    private InventoryItem inventoryItem;

    @BeforeEach
    public void setUp() {
        // Setting up an InventoryItem object before each test.
        inventoryItem = new InventoryItem("Test Item", "Test Item", "Test Description", 10, "Test Tag");
    }


    @Test
    public void testParameterizedConstructor() {
        // Testing parameterized constructor
        assertEquals("Test Item", inventoryItem.getId());
        assertEquals("Test Item", inventoryItem.getName());
        assertEquals("Test Description", inventoryItem.getDescription());
        assertEquals(10, inventoryItem.getQuantity());
        assertEquals("Test Tag", inventoryItem.getTag());
    }


    @Test
    public void testCompareTo() {
        // Testing equality in the compareTo method
        InventoryItem anotherItem = new InventoryItem("Another Item", "Another Item", "Description", 5, "Tag");
        anotherItem.setName("Test Item");

        assertEquals(0, inventoryItem.compareTo(anotherItem));
    }


    @Test
    public void testCompareToGreater() {
        // Testing the compareTo method
        InventoryItem anotherItem = new InventoryItem("Alpha Item", "Alpha Item", "Description", 5, "Tag");

        // "Test Item" is lexicographically greater than "Another Item"
        assertTrue(inventoryItem.compareTo(anotherItem) > 0);
    }


    @Test
    public void testCompareToLess() {
        InventoryItem anotherItem = new InventoryItem("Zulu Item", "Zulu Item", "Description", 5, "Tag");

        // "Test Item" is lexicographically less than "Zebra Item"
        assertTrue(inventoryItem.compareTo(anotherItem) < 0);
    }


    @Test
    public void testCompareToOrder() {
        InventoryItem firstItem = new InventoryItem("Alpha Item", "Alpha Item", "Description", 5, "Tag");
        InventoryItem midItem = new InventoryItem("Lambda Item", "Lambda Item", "Description", 5, "Tag");
        InventoryItem lastItem = new InventoryItem("Zulu Item", "Zulu Item", "Description", 5, "Tag");

        ArrayList<InventoryItem> inventoryItemArrayList = new ArrayList<>();
        inventoryItemArrayList.add(firstItem);
        inventoryItemArrayList.add(inventoryItem);
        inventoryItemArrayList.add(lastItem);

        inventoryItemArrayList.sort(InventoryItem::compareTo);

        assertEquals(firstItem.getName(), inventoryItemArrayList.get(0).getName());
        assertEquals(inventoryItem.getName(), inventoryItemArrayList.get(1).getName());
        assertEquals(lastItem.getName(), inventoryItemArrayList.get(2).getName());

        // Test adding a new item
        inventoryItemArrayList.add(midItem);
        inventoryItemArrayList.sort(InventoryItem::compareTo);

        assertEquals(firstItem.getName(), inventoryItemArrayList.get(0).getName());
        assertEquals(midItem.getName(), inventoryItemArrayList.get(1).getName());
        assertEquals(inventoryItem.getName(), inventoryItemArrayList.get(2).getName());
        assertEquals(lastItem.getName(), inventoryItemArrayList.get(3).getName());


    }

}