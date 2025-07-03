package com.tk.pennywise


data class LoginStreak(
    var userEmail: String = "",
    var lastLoginDate: Long = 0L,
    var totalLoginDaysThisYear: Int = 0,
    var streak: Int = 0
)