package com.tk.pennywise

data class Feedback(
    val message: String = "",
    val email: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)