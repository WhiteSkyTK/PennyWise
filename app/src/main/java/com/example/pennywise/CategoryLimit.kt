package com.example.pennywise

import androidx.room.*

@Entity(
    tableName = "category_limits",
    indices = [Index(value = ["month", "category"], unique = true)]
)
data class CategoryLimit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: String,
    val category: String,
    val minAmount: Double,
    val maxAmount: Double,
    val usedAmount: Double
)


