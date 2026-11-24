package com.example.famekodriver.core.utils

import kotlin.math.roundToInt

object CurrencyUtils {

    /**
     * Formats a monetary amount as whole Ghana Cedis for presentation.
     * Examples:
     * - 25.00 -> "GH₵25"
     * - 25.49 -> "GH₵25"
     * - 25.50 -> "GH₵26"
     * - 35.75 -> "GH₵36"
     * - 42.49 -> "GH₵42"
     * - 99.99 -> "GH₵100"
     * - 100.00 -> "GH₵100"
     * - 100.50 -> "GH₵101"
     */
    fun formatCedi(amount: Double?): String {
        if (amount == null || amount.isNaN() || amount.isInfinite()) {
            return "GH₵0"
        }
        val rounded = amount.roundToInt()
        return "GH₵$rounded"
    }

    /**
     * Formats a monetary amount with a space between currency symbol and value.
     * Example: 25.00 -> "GH₵ 25"
     */
    fun formatCediWithSpace(amount: Double?): String {
        if (amount == null || amount.isNaN() || amount.isInfinite()) {
            return "GH₵ 0"
        }
        val rounded = amount.roundToInt()
        return "GH₵ $rounded"
    }

    /**
     * Returns the rounded whole cedi integer value.
     */
    fun formatWholeCediAmount(amount: Double?): Int {
        if (amount == null || amount.isNaN() || amount.isInfinite()) {
            return 0
        }
        return amount.roundToInt()
    }
}
