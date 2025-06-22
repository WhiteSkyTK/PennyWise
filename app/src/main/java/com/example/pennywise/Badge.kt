package com.example.pennywise

import androidx.room.*

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val iconResId: Int = 0,
    val isEarned: Boolean = false,
    val overlayText: String? = null
)