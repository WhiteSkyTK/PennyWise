package com.example.pennywise

import androidx.room.*

@Entity(tableName = "login_streaks")
data class LoginStreak(
    @PrimaryKey val userEmail: String,
    val lastLoginDate: Long,
    val totalLoginDaysThisYear: Int,
    val streak: Int
)