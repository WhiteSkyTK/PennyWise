package com.example.pennywise

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String?,
    val date: Long,
    val startTime: String,
    val endTime: String,
    val photoUri: String? = null
)


