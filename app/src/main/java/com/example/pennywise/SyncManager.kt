package com.example.pennywise

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(
    private val transactionRepo: TransactionRepository
) {
    suspend fun syncTransactionsIfNeeded() = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncManager", "Checking if local RoomDB has transactions for sync")
            val localTransactions = transactionRepo.getLocalTransactions()

            if (localTransactions.isNotEmpty()) {
                Log.d("SyncManager", "Found ${localTransactions.size} local transactions. Pushing to Firestore...")
                transactionRepo.syncToFirestore()
            } else {
                Log.d("SyncManager", "No local data found. Pulling from Firebase...")
                transactionRepo.syncFromFirestore()
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error during sync", e)
        }
    }
}
