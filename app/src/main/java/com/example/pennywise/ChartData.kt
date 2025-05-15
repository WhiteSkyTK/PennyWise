package com.example.pennywise

data class ChartData(
    val category: String,
    val value: Double,
    val min: Double? = null,
    val max: Double? = null
)