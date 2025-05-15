package com.example.pennywise

import androidx.room.Dao
import androidx.room.Delete
import android.util.Log
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryLimitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLimit(limit: CategoryLimit)

    @Query("SELECT * FROM category_limits WHERE month = :month")
    suspend fun getCategoryLimits(month: String): List<CategoryLimit>

    @Query("DELETE FROM category_limits WHERE id = :id")
    suspend fun deleteCategoryLimit(id: Int)

    @Query("SELECT * FROM category_limits WHERE month IN (:months)")
    suspend fun getLimitsForMonths(months: List<String>): List<CategoryLimit>

    @Query("""
    SELECT COALESCE(SUM(amount), 0) 
    FROM transactions 
    WHERE category = :category 
    AND type = 'expense'
    AND strftime('%Y-%m', datetime(date / 1000, 'unixepoch')) = :month
""")
    suspend fun getUsedAmountForCategory(month: String, category: String): Double

    @Query("SELECT * FROM category_limits WHERE month = :month AND category = :category")
    suspend fun getCategoryLimit(month: String, category: String): CategoryLimit?
}


