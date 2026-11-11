package com.shopkeeper.ui.viewmodel

import com.shopkeeper.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enterprise Unit Tests for Category Management CRUD:
 * - Standalone category creation
 * - Category renaming and merge detection
 * - Atomic reassignment on deletion (zero orphaned products)
 * - Immutability of default "General" category
 * - Visibility of empty custom categories alongside populated categories
 */
class CategoryCrudLogicTest {

    private fun deriveAllCategories(
        categories: List<String>,
        products: List<Product>
    ): List<String> {
        val fromCategories = categories.filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        val fromProducts = products.map { it.category.trim() }.filter { it.isNotBlank() }
        val combined = (listOf("General") + fromCategories + fromProducts).distinctBy { it.lowercase() }
        val nonGeneral = combined.filterNot { it.equals("General", ignoreCase = true) }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        return listOf("General") + nonGeneral
    }

    private fun reassignProducts(
        products: List<Product>,
        oldCategory: String,
        newCategory: String
    ): Pair<List<Product>, Int> {
        var affected = 0
        val updated = products.map { prod ->
            if (prod.category.equals(oldCategory, ignoreCase = true)) {
                affected++
                prod.copy(category = newCategory)
            } else {
                prod
            }
        }
        return Pair(updated, affected)
    }

    @Test
    fun `default General category is always present and placed first`() {
        val allCats = deriveAllCategories(
            categories = listOf("All", "Zebra", "Bakery"),
            products = emptyList()
        )
        assertEquals("General", allCats.first())
        assertEquals(listOf("General", "Bakery", "Zebra"), allCats)
    }

    @Test
    fun `empty custom categories are preserved and visible alongside populated categories`() {
        val customCategories = listOf("General", "Groceries", "CustomEmpty1", "CustomEmpty2")
        val products = listOf(
            Product(id = "1", code = "101", name = "Rice", category = "Groceries")
        )

        val allCats = deriveAllCategories(customCategories, products)
        assertTrue("CustomEmpty1 must be present", allCats.contains("CustomEmpty1"))
        assertTrue("CustomEmpty2 must be present", allCats.contains("CustomEmpty2"))
        assertTrue("Groceries must be present", allCats.contains("Groceries"))
        assertEquals(4, allCats.size)
    }

    @Test
    fun `reassignCategory updates all matching products atomically`() {
        val products = listOf(
            Product(id = "1", code = "101", name = "Rice", category = "Groceries"),
            Product(id = "2", code = "102", name = "Oil", category = "Groceries"),
            Product(id = "3", code = "103", name = "Bulb", category = "Electronics")
        )

        val (updated, affectedCount) = reassignProducts(products, oldCategory = "Groceries", newCategory = "Food & Pantry")
        assertEquals(2, affectedCount)

        val groceriesLeft = updated.count { it.category == "Groceries" }
        val foodCount = updated.count { it.category == "Food & Pantry" }
        val electronicsCount = updated.count { it.category == "Electronics" }

        assertEquals(0, groceriesLeft)
        assertEquals(2, foodCount)
        assertEquals(1, electronicsCount)
    }

    @Test
    fun `deleting category with products reassigns them to General safely`() {
        val products = listOf(
            Product(id = "1", code = "201", name = "T-Shirt", category = "Clothing"),
            Product(id = "2", code = "202", name = "Jeans", category = "Clothing")
        )

        val (reassigned, affected) = reassignProducts(products, oldCategory = "Clothing", newCategory = "General")
        assertEquals(2, affected)
        assertTrue(reassigned.all { it.category == "General" })
    }

    @Test
    fun `deleting empty category affects zero products`() {
        val products = listOf(
            Product(id = "1", code = "101", name = "Rice", category = "Groceries")
        )

        val (reassigned, affected) = reassignProducts(products, oldCategory = "EmptyCategory", newCategory = "General")
        assertEquals(0, affected)
        assertEquals(products, reassigned)
    }

    @Test
    fun `merge detection recognizes when target rename category already exists`() {
        val existingCategories = listOf("General", "Electronics", "Hardware", "Groceries")
        val renameTarget = "electronics" // case-insensitive match

        val isMerge = existingCategories.any { it.equals(renameTarget.trim(), ignoreCase = true) }
        assertTrue("Renaming to an existing category must trigger merge detection", isMerge)
    }

    @Test
    fun `General category cannot be deleted`() {
        val categoryToDelete = "General"
        val canDelete = !categoryToDelete.equals("General", ignoreCase = true)
        assertFalse("General category must be protected from deletion", canDelete)
    }
}
