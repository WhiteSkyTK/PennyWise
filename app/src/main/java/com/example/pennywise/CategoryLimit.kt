package com.example.pennywise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_limits")
data class CategoryLimit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: String,
    val category: String,
    val limit: Double
)

