package com.example.pennywise

import androidx.room.*

@Entity(tableName = "earned_badges")
data class EarnedBadge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val badgeTitle: String,
    val earnedTimestamp: Long,
    val metadata: Int? = null // default 1, e.g., number of times or count for the badge
)