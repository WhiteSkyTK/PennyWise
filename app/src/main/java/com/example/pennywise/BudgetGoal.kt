package com.example.pennywise

data class BudgetGoal(
    var month: String = "",        // e.g., "2025-05"
    var minAmount: Double = 0.0,
    var maxAmount: Double = 0.0
)