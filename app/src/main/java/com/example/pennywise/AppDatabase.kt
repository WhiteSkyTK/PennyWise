package com.example.pennywise

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        Transaction::class,
        Category::class,
        CategoryLimit::class,
        BudgetGoal::class,
        EarnedBadge::class,
        Badge::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun badgeDao(): BadgeDao
    abstract fun goalDao(): BudgetGoalDao
    abstract fun limitDao(): CategoryLimitDao
    abstract fun earnedBadgeDao(): EarnedBadgeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pennywise.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}