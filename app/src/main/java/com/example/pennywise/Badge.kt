package com.example.pennywise

data class Badge(
    val title: String,
    val description: String,
    val iconResId: Int,
    val isEarned: Boolean = false,
    val overlayText: String? = null
)