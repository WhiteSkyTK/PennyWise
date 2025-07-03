package com.tk.pennywise

import androidx.room.*

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey var month: String = "",        // e.g., "2025-05"
    var minAmount: Double = 0.0,
    var maxAmount: Double = 0.0
)