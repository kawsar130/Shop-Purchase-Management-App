package com.shopkeeper.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCalculationTest {

    @Test
    fun `profit calculates unit profit in minor units and major units correctly`() {
        // Purchase: $100.00 (10000 minor), Sales: $135.00 (13500 minor)
        val product = Product(purchasePriceMinor = 10000L, salesPriceMinor = 13500L)
        assertEquals(3500L, product.profitMinor)
        assertEquals(35.0, product.profit, 0.001)

        // Loss: Purchase $200.00, Sales $180.00
        val lossProduct = Product(purchasePriceMinor = 20000L, salesPriceMinor = 18000L)
        assertEquals(-2000L, lossProduct.profitMinor)
        assertEquals(-20.0, lossProduct.profit, 0.001)

        // Break-even
        val breakEvenProduct = Product(purchasePriceMinor = 5000L, salesPriceMinor = 5000L)
        assertEquals(0L, breakEvenProduct.profitMinor)
        assertEquals(0.0, breakEvenProduct.profit, 0.001)
    }

    @Test
    fun `markupPercentage computes markup on purchase cost`() {
        // Normal product: Cost 200.00, Sales 250.00 -> Markup = (50 / 200) * 100 = 25.0%
        val normalProduct = Product(purchasePriceMinor = 20000L, salesPriceMinor = 25000L)
        assertEquals(25.0, normalProduct.markupPercentage, 0.001)

        // Zero cost product: Cost 0, Sales 50 -> 100% markup
        val zeroCostProduct = Product(purchasePriceMinor = 0L, salesPriceMinor = 5000L)
        assertEquals(100.0, zeroCostProduct.markupPercentage, 0.001)

        // Zero price and cost -> 0%
        val zeroPriceAndCost = Product(purchasePriceMinor = 0L, salesPriceMinor = 0L)
        assertEquals(0.0, zeroPriceAndCost.markupPercentage, 0.001)
    }

    @Test
    fun `profitMarginPercentage computes gross margin on sales price`() {
        // Cost 200.00, Sales 250.00 -> Profit = 50. Margin = (50 / 250) * 100 = 20.0%
        val normalProduct = Product(purchasePriceMinor = 20000L, salesPriceMinor = 25000L)
        assertEquals(20.0, normalProduct.profitMarginPercentage, 0.001)

        // Cost 100.00, Sales 125.00 -> Margin = (25 / 125) * 100 = 20.0%
        val retailProduct = Product(purchasePriceMinor = 10000L, salesPriceMinor = 12500L)
        assertEquals(20.0, retailProduct.profitMarginPercentage, 0.001)

        // Zero cost: Cost 0, Sales 50 -> (50 / 50) * 100 = 100.0%
        val zeroCostProduct = Product(purchasePriceMinor = 0L, salesPriceMinor = 5000L)
        assertEquals(100.0, zeroCostProduct.profitMarginPercentage, 0.001)

        // Zero sales price -> 0.0%
        val zeroSales = Product(purchasePriceMinor = 5000L, salesPriceMinor = 0L)
        assertEquals(0.0, zeroSales.profitMarginPercentage, 0.001)
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
    fun `totalPurchaseValue and totalSalesValue evaluate correctly with exact minor precision`() {
        val product = Product(purchasePriceMinor = 1550L, salesPriceMinor = 2025L, quantity = 10)
        assertEquals(15500L, product.totalPurchaseValueMinor)
        assertEquals(155.0, product.totalPurchaseValue, 0.001)
        assertEquals(20250L, product.totalSalesValueMinor)
        assertEquals(202.5, product.totalSalesValue, 0.001)

        val negativeStockProduct = Product(purchasePriceMinor = 1500L, salesPriceMinor = 2000L, quantity = -5)
        assertEquals(0L, negativeStockProduct.totalPurchaseValueMinor)
        assertEquals(0.0, negativeStockProduct.totalPurchaseValue, 0.001)
    }

    @Test
    fun `fromMajorUnits helper converts Double prices accurately to minor units`() {
        val p = Product.fromMajorUnits(purchasePrice = 12.34, salesPrice = 45.67, quantity = 5)
        assertEquals(1234L, p.purchasePriceMinor)
        assertEquals(4567L, p.salesPriceMinor)
    }
}
