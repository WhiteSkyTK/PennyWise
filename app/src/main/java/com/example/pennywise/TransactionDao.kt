package com.example.pennywise

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC, startTime DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("""
    SELECT * FROM transactions 
    WHERE userEmail = :email 
    AND strftime('%m', CAST(date / 1000 AS INTEGER), 'unixepoch') = :month 
    AND strftime('%Y', CAST(date / 1000 AS INTEGER), 'unixepoch') = :year
    ORDER BY date DESC, startTime DESC
""")
    suspend fun getTransactionsByUserAndMonth(email: String, month: String, year: String): List<Transaction>

    @Query("""
    SELECT IFNULL(SUM(amount), 0) 
    FROM transactions 
    WHERE category = :category 
      AND type = 'Expense' 
      AND strftime('%m-%Y', datetime(date / 1000, 'unixepoch')) = :month
""")
    suspend fun getUsedAmountForCategory(month: String, category: String): Double

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE userEmail = :userEmail AND type = :type GROUP BY category")
    suspend fun getTotalSpentPerCategory(userEmail: String, type: String): List<CategoryTotal>

}
