package com.example.pennywise

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD,
    VERY_HARD
}

data class Badge(
    val id: Int,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val iconResId: Int,
    val isEarned: Boolean = false,
    val overlayText: String? = null
)
