package com.tk.pennywise


import androidx.room.*

@Dao
interface CategoryLimitDao {
    @Query("SELECT * FROM category_limits")
    suspend fun getAll(): List<CategoryLimit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(limit: CategoryLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(limits: List<CategoryLimit>)

    @Query("DELETE FROM category_limits")
    suspend fun deleteAll()
}
