package com.example.pennywise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pennywise.Category
import com.example.pennywise.CategoryDao
import com.example.pennywise.Transaction
import com.example.pennywise.TransactionDao
import com.example.pennywise.User
import com.example.pennywise.BudgetGoal
import com.example.pennywise.CategoryLimit
import com.example.pennywise.BudgetGoalDao
import com.example.pennywise.CategoryLimitDao

@Database(entities = [User::class, Transaction::class, Category::class, BudgetGoal::class, CategoryLimit::class], version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun categoryLimitDao(): CategoryLimitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the correct structure, ensuring the id column is NOT NULL
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                name TEXT NOT NULL, 
                type TEXT NOT NULL
            )
        """)

                // Copy the data from the old table to the new one (ignoring the userEmail)
                database.execSQL("""
            INSERT INTO categories_new (id, name, type) 
            SELECT id, name, type FROM categories
        """)

                // Drop the old table
                database.execSQL("DROP TABLE categories")

                // Rename the new table to the original table name
                database.execSQL("ALTER TABLE categories_new RENAME TO categories")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pennywise_db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

