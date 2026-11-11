package com.shopkeeper.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Category Resolution, Merging, Deduplication, and Zero-State Trailing Chip UX.
 */
class CategoryResolutionLogicTest {

    private fun resolveCatalogCategories(
        productCategories: List<String>,
        customCategories: Set<String>
    ): List<String> {
        val cleanProductCats = productCategories.map { it.trim() }.filter { it.isNotBlank() }
        val allDistinct = (cleanProductCats + customCategories)
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        return if (allDistinct.isEmpty()) {
            emptyList()
        } else {
            listOf("All") + allDistinct
        }
    }

    private fun resolvePickerCleanCategories(
        availableCategories: List<String>,
        localCreatedCategories: Set<String>
    ): List<String> {
        val list = (availableCategories + localCreatedCategories)
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .distinctBy { it.lowercase() }
        val sortedNonGeneral = list
            .filter { !it.equals("General", ignoreCase = true) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        return listOf("General") + sortedNonGeneral
    }

    private fun resolveTopChips(cleanCategories: List<String>, selectedCategory: String): List<String> {
        val top = cleanCategories.take(5).toMutableList()
        if (selectedCategory.isNotBlank() && !top.any { it.equals(selectedCategory, ignoreCase = true) }) {
            top.add(0, selectedCategory)
        }
        return top.distinct().take(5)
    }

    @Test
    fun `empty inventory and empty custom categories returns empty catalog categories`() {
        val catalog = resolveCatalogCategories(emptyList(), emptySet())
        assertTrue(catalog.isEmpty())
    }

    @Test
    fun `empty inventory produces General as sole picker category with zero remaining`() {
        val clean = resolvePickerCleanCategories(availableCategories = emptyList(), localCreatedCategories = emptySet())
        assertEquals(listOf("General"), clean)

        val top = resolveTopChips(clean, selectedCategory = "General")
        assertEquals(listOf("General"), top)

        val remainingCount = (clean.size - top.size).coerceAtLeast(0)
        assertEquals(0, remainingCount)
    }

    @Test
    fun `creating new category joins picker immediately and does not vanish when changing selection`() {
        // Initial state: empty available categories
        val localCreated = mutableSetOf<String>()

        // User types and creates "Mobile"
        localCreated.add("Mobile")

        val cleanWithMobile = resolvePickerCleanCategories(
            availableCategories = emptyList(),
            localCreatedCategories = localCreated
        )
        // Expected: ["General", "Mobile"]
        assertEquals(listOf("General", "Mobile"), cleanWithMobile)

        // When "Mobile" is selected
        val topSelectedMobile = resolveTopChips(cleanWithMobile, selectedCategory = "Mobile")
        assertTrue(topSelectedMobile.contains("Mobile"))

        // When user changes selection back to "General"
        val topSelectedGeneral = resolveTopChips(cleanWithMobile, selectedCategory = "General")
        // "Mobile" must still remain in top chips!
        assertTrue("Mobile must remain in chips after selecting General", topSelectedGeneral.contains("Mobile"))
        assertEquals(listOf("General", "Mobile"), topSelectedGeneral)
    }

    @Test
    fun `categories are combined and deduplicated case-insensitively`() {
        val productCats = listOf("Electronics", "Groceries", "electronics")
        val customCats = setOf("Hardware", "GROCERIES", "Stationery")

        val catalog = resolveCatalogCategories(productCats, customCats)
        assertEquals("All", catalog.first())

        val categoriesWithoutAll = catalog.drop(1)
        assertEquals(4, categoriesWithoutAll.size)
        // Case-insensitive distinct sorted
        assertEquals(listOf("Electronics", "Groceries", "Hardware", "Stationery"), categoriesWithoutAll)
    }

    @Test
    fun `picker places General first regardless of alphabetical sorting`() {
        val available = listOf("All", "Zebra Accessories", "Automotive", "General", "Bakery")
        val clean = resolvePickerCleanCategories(available, emptySet())

        assertEquals("General", clean.first())
        assertEquals(listOf("General", "Automotive", "Bakery", "Zebra Accessories"), clean)
    }

    @Test
    fun `overflow trailing chip calculation correctly computes remaining count`() {
        val categories = listOf("General", "Cat1", "Cat2", "Cat3", "Cat4", "Cat5", "Cat6", "Cat7")
        val top = resolveTopChips(categories, selectedCategory = "General")
        assertEquals(5, top.size)

        val remainingCount = (categories.size - top.size).coerceAtLeast(0)
        assertEquals(3, remainingCount)
    }
}
