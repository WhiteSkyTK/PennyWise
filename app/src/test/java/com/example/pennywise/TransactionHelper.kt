package com.example.pennywise

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TransactionHelper {

    fun validateAmount(amount: String): Double {
        val cleanedAmount = amount.replace("R", "").toDoubleOrNull()
        return cleanedAmount ?: 0.0 // Return 0.0 if the format is invalid
    }

    fun isValidCategory(selectedCategory: String): Boolean {
        return selectedCategory.isNotEmpty() && selectedCategory != "Please select a category"
    }

    fun getCurrentFormattedTime(): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    fun getFormattedDate(calendar: Calendar): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}