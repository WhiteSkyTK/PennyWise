package com.tk.pennywise

import androidx.room.*

@Entity(tableName = "category_limits")
data class CategoryLimit(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val category: String = "",
    val month: String = "",    // e.g. "2025-05"
    val minAmount: Double = 0.0,
    val maxAmount: Double = 0.0,
    var usedAmount: Double = 0.0
)
