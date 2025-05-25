package com.example.pennywise

data class ChartData(
    var category: String = "",
    var value: Double = 0.0,
    var min: Double? = null,
    var max: Double? = null
)