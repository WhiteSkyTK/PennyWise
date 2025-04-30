package com.example.pennywise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey val month: String, // e.g. "2025-04"
    val minAmount: Double,
    val maxAmount: Double
)

