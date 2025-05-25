package com.example.pennywise

data class Badge(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val iconResId: Int = 0,
    val isEarned: Boolean = false,
    val overlayText: String? = null
)