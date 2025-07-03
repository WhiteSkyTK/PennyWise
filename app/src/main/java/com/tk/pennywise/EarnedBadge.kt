package com.tk.pennywise


import androidx.room.*

@Entity(tableName = "earned_badges")
data class EarnedBadge(
    @PrimaryKey var id: Int = 0,
    var userEmail: String = "",
    var badgeTitle: String = "",
    var earnedTimestamp: Long = 0L,
    var metadata: Int? = null
)
