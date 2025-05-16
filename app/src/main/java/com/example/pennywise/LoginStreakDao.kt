package com.example.pennywise

import androidx.room.*

@Dao
interface LoginStreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(streak: LoginStreak)

    @Query("SELECT * FROM login_streaks WHERE userEmail = :email")
    suspend fun getStreak(email: String): LoginStreak?
}
