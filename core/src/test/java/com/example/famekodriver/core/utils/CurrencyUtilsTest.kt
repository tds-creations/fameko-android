package com.example.famekodriver.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun testWholeCediFormatting_StandardValues() {
        assertEquals("GH₵25", CurrencyUtils.formatCedi(25.00))
        assertEquals("GH₵25", CurrencyUtils.formatCedi(25.49))
        assertEquals("GH₵26", CurrencyUtils.formatCedi(25.50))
        assertEquals("GH₵26", CurrencyUtils.formatCedi(25.75))
        assertEquals("GH₵36", CurrencyUtils.formatCedi(35.75))
        assertEquals("GH₵42", CurrencyUtils.formatCedi(42.49))
        assertEquals("GH₵100", CurrencyUtils.formatCedi(99.99))
        assertEquals("GH₵100", CurrencyUtils.formatCedi(100.00))
        assertEquals("GH₵101", CurrencyUtils.formatCedi(100.50))
    }

    @Test
    fun testWholeCediFormatting_WithSpace() {
        assertEquals("GH₵ 25", CurrencyUtils.formatCediWithSpace(25.00))
        assertEquals("GH₵ 25", CurrencyUtils.formatCediWithSpace(25.49))
        assertEquals("GH₵ 26", CurrencyUtils.formatCediWithSpace(25.50))
        assertEquals("GH₵ 26", CurrencyUtils.formatCediWithSpace(25.75))
        assertEquals("GH₵ 36", CurrencyUtils.formatCediWithSpace(35.75))
        assertEquals("GH₵ 42", CurrencyUtils.formatCediWithSpace(42.49))
        assertEquals("GH₵ 100", CurrencyUtils.formatCediWithSpace(99.99))
        assertEquals("GH₵ 100", CurrencyUtils.formatCediWithSpace(100.00))
        assertEquals("GH₵ 101", CurrencyUtils.formatCediWithSpace(100.50))
    }

    @Test
    fun testWholeCediFormatting_EdgeAndNullCases() {
        assertEquals("GH₵0", CurrencyUtils.formatCedi(null))
        assertEquals("GH₵0", CurrencyUtils.formatCedi(Double.NaN))
        assertEquals("GH₵0", CurrencyUtils.formatCedi(Double.POSITIVE_INFINITY))
        assertEquals("GH₵0", CurrencyUtils.formatCedi(0.0))
    }
}
