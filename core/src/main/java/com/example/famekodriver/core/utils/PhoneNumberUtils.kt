package com.example.famekodriver.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

object PhoneNumberUtils {

    /**
     * Normalizes a Ghanaian or international phone number into standard dialable format.
     * Examples:
     * - "0241234567" -> "+233241234567"
     * - "0551234567" -> "+233551234567"
     * - "+233241234567" -> "+233241234567"
     * - "233241234567" -> "+233241234567"
     */
    fun normalizePhoneNumber(rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null

        // Strip all spaces, dashes, brackets, and non-numeric characters except leading '+'
        val clean = rawPhone.trim().replace(Regex("[^0-9+]"), "")
        if (clean.isBlank()) return null

        return when {
            // Already E.164 international format
            clean.startsWith("+") -> clean
            // Local Ghanaian format starting with '0' (e.g., 024XXXXXXX, 055XXXXXXX, 020XXXXXXX)
            clean.startsWith("0") && clean.length == 10 -> "+233" + clean.substring(1)
            // Ghanaian format without leading '+' (e.g., 23324XXXXXXX)
            clean.startsWith("233") && clean.length == 12 -> "+$clean"
            // Fallback: Return clean digits if valid length
            clean.length >= 7 -> clean
            else -> null
        }
    }

    /**
     * Opens the Android system phone dialer (`ACTION_DIAL`) with the pre-populated phone number.
     * Does NOT require dangerous CALL_PHONE permission.
     */
    fun openDialer(context: Context, rawPhone: String?, recipientLabel: String = "User") {
        val normalized = normalizePhoneNumber(rawPhone)
        if (normalized.isNullOrBlank()) {
            Log.w("FamekoDialer", "Unable to dial: No valid phone number for $recipientLabel")
            Toast.makeText(context, "No phone number available for $recipientLabel.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            Log.d("FamekoDialer", "Opening system dialer for recipient $recipientLabel")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$normalized")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("FamekoDialer", "Failed to launch Android system phone dialer", e)
            Toast.makeText(context, "Could not open phone dialer.", Toast.LENGTH_SHORT).show()
        }
    }
}
