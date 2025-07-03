package com.tk.pennywise


import androidx.room.*

@Dao
interface EarnedBadgeDao {
    @Query("SELECT * FROM earned_badges")
    suspend fun getAll(): List<EarnedBadge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(badges: List<EarnedBadge>)

    @Query("DELETE FROM earned_badges")
    suspend fun deleteAll()
}
