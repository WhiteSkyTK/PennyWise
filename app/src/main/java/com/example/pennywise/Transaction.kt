package com.example.pennywise

data class Transaction(
    var id: String = "", // Firebase will generate this as a document ID or you assign it
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "", // income or expense
    val category: String = "",
    val categoryId: String = "",
    val description: String? = null,
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val photoPath: String? = null,
    val monthYear: String = ""
)