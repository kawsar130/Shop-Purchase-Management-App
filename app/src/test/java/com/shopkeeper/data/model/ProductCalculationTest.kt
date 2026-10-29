package com.shopkeeper.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCalculationTest {

    @Test
    fun `profit calculates unit profit correctly`() {
        val product = Product(purchasePrice = 100.0, salesPrice = 135.0)
        assertEquals(35.0, product.profit, 0.001)

        val lossProduct = Product(purchasePrice = 200.0, salesPrice = 180.0)
        assertEquals(-20.0, lossProduct.profit, 0.001)

        val breakEvenProduct = Product(purchasePrice = 50.0, salesPrice = 50.0)
        assertEquals(0.0, breakEvenProduct.profit, 0.001)
    }

    @Test
    fun `profitMarginPercentage computes markup on purchase price`() {
        val normalProduct = Product(purchasePrice = 200.0, salesPrice = 250.0)
        // (250 - 200) / 200 * 100 = 25.0%
        assertEquals(25.0, normalProduct.profitMarginPercentage, 0.001)

        val zeroCostProduct = Product(purchasePrice = 0.0, salesPrice = 50.0)
        assertEquals(100.0, zeroCostProduct.profitMarginPercentage, 0.001)

        val zeroPriceAndCost = Product(purchasePrice = 0.0, salesPrice = 0.0)
        assertEquals(0.0, zeroPriceAndCost.profitMarginPercentage, 0.001)
    }

    @Test
    fun `isLowStock accurately evaluates stock boundaries`() {
        assertFalse("0 stock is out of stock, not low stock", Product(quantity = 0).isLowStock)
        assertTrue("1 stock is low stock", Product(quantity = 1).isLowStock)
        assertTrue("3 stock is low stock", Product(quantity = 3).isLowStock)
        assertTrue("5 stock is low stock", Product(quantity = 5).isLowStock)
        assertFalse("6 stock is normal stock", Product(quantity = 6).isLowStock)
        assertFalse("Negative quantity is not low stock", Product(quantity = -1).isLowStock)
    }

    @Test
    fun `totalPurchaseValue and totalSalesValue evaluate correctly`() {
        val product = Product(purchasePrice = 15.0, salesPrice = 20.0, quantity = 10)
        assertEquals(150.0, product.totalPurchaseValue, 0.001)
        assertEquals(200.0, product.totalSalesValue, 0.001)

        val negativeStockProduct = Product(purchasePrice = 15.0, salesPrice = 20.0, quantity = -5)
        assertEquals(0.0, negativeStockProduct.totalPurchaseValue, 0.001)
        assertEquals(0.0, negativeStockProduct.totalSalesValue, 0.001)
    }
}
