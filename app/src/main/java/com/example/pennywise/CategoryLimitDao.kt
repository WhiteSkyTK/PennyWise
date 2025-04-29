package com.example.pennywise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryLimitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLimit(limit: CategoryLimit)

    @Query("SELECT * FROM category_limits WHERE month = :month")
    suspend fun getCategoryLimits(month: String): List<CategoryLimit>

    @Query("SELECT * FROM category_limits WHERE month = :month AND category = :category")
    suspend fun getCategoryLimit(month: String, category: String): CategoryLimit?
}
