package com.tk.pennywise

import androidx.room.*

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey var id: String = "",              // Firestore ID
    var name: String = "",
    var type: String = "",            // "Income" or "Expense"
    var categoryIndex: Int? = null    // Optional
)
