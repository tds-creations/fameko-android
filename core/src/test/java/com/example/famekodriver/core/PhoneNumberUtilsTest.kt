package com.example.famekodriver.core

import com.example.famekodriver.core.utils.PhoneNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberUtilsTest {

    @Test
    fun testGhanaianLocalNumberNormalization() {
        assertEquals("+233241234567", PhoneNumberUtils.normalizePhoneNumber("0241234567"))
        assertEquals("+233551234567", PhoneNumberUtils.normalizePhoneNumber("0551234567"))
        assertEquals("+233201234567", PhoneNumberUtils.normalizePhoneNumber("0201234567"))
    }

    @Test
    fun testInternationalNumberNormalization() {
        assertEquals("+233241234567", PhoneNumberUtils.normalizePhoneNumber("+233241234567"))
        assertEquals("+233241234567", PhoneNumberUtils.normalizePhoneNumber("233241234567"))
    }

    @Test
    fun testFormattedStringNormalization() {
        assertEquals("+233241234567", PhoneNumberUtils.normalizePhoneNumber("+233 24 123 4567"))
        assertEquals("+233241234567", PhoneNumberUtils.normalizePhoneNumber("(024) 123-4567"))
    }

    @Test
    fun testNullOrInvalidNumbers() {
        assertNull(PhoneNumberUtils.normalizePhoneNumber(null))
        assertNull(PhoneNumberUtils.normalizePhoneNumber(""))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("   "))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("123"))
    }
}
