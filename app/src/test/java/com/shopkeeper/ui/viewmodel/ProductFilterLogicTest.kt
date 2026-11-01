package com.shopkeeper.ui.viewmodel

import com.shopkeeper.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductFilterLogicTest {

    private val sampleProducts = listOf(
        Product(id = "1", code = "101", name = "Miniket Rice 5kg", category = "Groceries"),
        Product(id = "2", code = "102", name = "Soybean Cooking Oil 1L", category = "Groceries"),
        Product(id = "3", code = "201", name = "LED Bulb 15W", category = "Electronics"),
        Product(id = "4", code = "202", name = "Extension Cord 3M", category = "Electronics"),
        Product(id = "5", code = "301", name = "Cotton Bath Towel", category = "Clothing")
    )

    private fun filterProducts(products: List<Product>, query: String, category: String): List<Product> {
        var list = products

        if (category != "All" && category.isNotBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotBlank()) {
            val strippedQuery = trimmedQuery.removePrefix("#")
            list = list.filter {
                it.name.contains(trimmedQuery, ignoreCase = true) ||
                it.code.contains(trimmedQuery, ignoreCase = true) ||
                (strippedQuery.isNotEmpty() && it.code.contains(strippedQuery, ignoreCase = true))
            }
        }

        return list
    }

    @Test
    fun `category filter returns all products when category is All`() {
        val result = filterProducts(sampleProducts, query = "", category = "All")
        assertEquals(5, result.size)
    }

    @Test
    fun `category filter isolates matching category case-insensitively`() {
        val result = filterProducts(sampleProducts, query = "", category = "groceries")
        assertEquals(2, result.size)
        assertEquals(listOf("101", "102"), result.map { it.code })
    }

    @Test
    fun `search filter matches by product name substring`() {
        val result = filterProducts(sampleProducts, query = "rice", category = "All")
        assertEquals(1, result.size)
        assertEquals("Miniket Rice 5kg", result.first().name)
    }

    @Test
    fun `search filter matches by product code with or without hash prefix`() {
        val withoutHash = filterProducts(sampleProducts, query = "201", category = "All")
        assertEquals(1, withoutHash.size)
        assertEquals("LED Bulb 15W", withoutHash.first().name)

        val withHash = filterProducts(sampleProducts, query = "#201", category = "All")
        assertEquals(1, withHash.size)
        assertEquals("LED Bulb 15W", withHash.first().name)
    }

    @Test
    fun `combined category and search query filters concurrently`() {
        val result = filterProducts(sampleProducts, query = "Cord", category = "Electronics")
        assertEquals(1, result.size)
        assertEquals("Extension Cord 3M", result.first().name)

        // Searching for "Cord" in "Groceries" should return empty
        val emptyResult = filterProducts(sampleProducts, query = "Cord", category = "Groceries")
        assertEquals(0, emptyResult.size)
    }

    private fun deriveCategories(products: List<Product>): List<String> {
        if (products.isEmpty()) return emptyList()
        val custom = products.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        return listOf("All") + custom
    }

    @Test
    fun `deriveCategories returns empty list when inventory has 0 products`() {
        val result = deriveCategories(emptyList())
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `deriveCategories prepends All when inventory has at least one product`() {
        val result = deriveCategories(sampleProducts)
        assertEquals(listOf("All", "Clothing", "Electronics", "Groceries"), result)
    }

    @Test
    fun `deriveCategories returns only All when all products have blank categories`() {
        val productsWithBlankCategories = listOf(
            Product(id = "1", code = "101", name = "Item A", category = ""),
            Product(id = "2", code = "102", name = "Item B", category = "   ")
        )
        val result = deriveCategories(productsWithBlankCategories)
        assertEquals(listOf("All"), result)
    }
}
