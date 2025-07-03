package com.tk.pennywise


import androidx.room.*

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    suspend fun getAll(): List<Badge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(badges: List<Badge>)

    @Query("DELETE FROM badges")
    suspend fun deleteAll()
}