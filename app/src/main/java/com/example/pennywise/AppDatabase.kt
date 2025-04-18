package com.example.pennywise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pennywise.Transaction
import com.example.pennywise.TransactionDao
import com.example.pennywise.User

@Database(entities = [User::class, Transaction::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pennywise_db"
                )
                    .fallbackToDestructiveMigration() // Optional: For development
                    .build().also { INSTANCE = it }
            }
        }
    }
}
