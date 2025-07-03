package com.tk.pennywise

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class TransactionHelperTest {

    @Test
    fun validateAmount_validInput_returnsDouble() {
        val result = TransactionHelper.validateAmount("R123.45")
        assertNotNull(result)
        assertEquals(123.45, result!!, 0.001)
    }

    @Test
    fun validateAmount_invalidInput_returnsNull() {
        val result = TransactionHelper.validateAmount("Rabc")
        assertNull(result)
    }

    @Test
    fun isValidCategory_valid_returnsTrue() {
        assertTrue(TransactionHelper.isValidCategory("Food"))
    }

    @Test
    fun isValidCategory_invalid_returnsFalse() {
        assertFalse(TransactionHelper.isValidCategory("Please select a category"))
        assertFalse(TransactionHelper.isValidCategory(""))
    }

    @Test
    fun getFormattedDate_returnsCorrectFormat() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 25)
        }
        val result = TransactionHelper.getFormattedDate(calendar)
        assertEquals("25 Dec 2024", result)
    }
}
