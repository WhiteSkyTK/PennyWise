package com.example.pennywise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var email: String,
    var password: String,
    var name: String? = null,
    var surname: String? = null,
    var profilePic: String? = null
)
