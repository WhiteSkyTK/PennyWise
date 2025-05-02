package com.example.pennywise

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsRaw(): List<Transaction>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsDebug(): List<Transaction>

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
      AND strftime('%m-%Y', datetime(date / 1000, 'unixepoch')) = :monthYear
""")
    suspend fun getUsedAmountForCategory(monthYear: String, category: String): Double

    @Query("""
    SELECT category AS category, SUM(amount) AS total 
    FROM transactions 
    WHERE userEmail = :userEmail 
      AND type = 'Expense'
      AND strftime('%m-%Y', datetime(date / 1000, 'unixepoch')) = :monthYear
    GROUP BY category
""")
    fun getMonthlyCategoryTotals(userEmail: String, monthYear: String): LiveData<List<CategoryTotal>>

    @Query("""
    SELECT IFNULL(SUM(amount), 0) 
    FROM transactions 
    WHERE category = :category 
      AND strftime('%m-%Y', datetime(date / 1000, 'unixepoch')) = :monthYear
""")
    suspend fun getUsedAmountForCategoryAllTypes(monthYear: String, category: String): Double

    @Query("""
    SELECT category, SUM(amount) as total FROM transactions
    WHERE date BETWEEN :startOfMonth AND :endOfMonth
    AND userEmail = :email AND type = 'expense' COLLATE NOCASE
    GROUP BY category
""")
    suspend fun getUsedAmountsByCategory(startOfMonth: Long, endOfMonth: Long, email: String): List<CategoryTotal>

}
