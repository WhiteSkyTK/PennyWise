package com.example.pennywise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val password: String,
    val name: String? = null,
    val surname: String? = null,
    val profilePic: String? = null
)
