package com.example.pennywise

data class Category(
    var id: String = "",              // Firestore ID
    var name: String = "",
    var type: String = "",            // "Income" or "Expense"
    var categoryIndex: Int? = null    // Optional
)
