package com.example.pennywise

data class EarnedBadge(
    var id: Int = 0,
    var userEmail: String = "",
    var badgeTitle: String = "",
    var earnedTimestamp: Long = 0L,
    var metadata: Int? = null
)
