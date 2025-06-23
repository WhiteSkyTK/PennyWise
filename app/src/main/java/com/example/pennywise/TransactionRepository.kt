package com.example.pennywise

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TransactionRepository(private val db: AppDatabase, private val userId: String) {

    private val firestore = FirebaseFirestore.getInstance()
    private val transactionsRef = firestore.collection("users").document(userId).collection("transactions")

    suspend fun getLocalTransactions(): List<Transaction> {
        return db.transactionDao().getAll()
    }
//sync functions
    suspend fun syncFromFirestore() {
        try {
            val snapshot = transactionsRef.get().await()
            val remoteTransactions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id, needsUpload = false)
            }
            db.transactionDao().insertAll(remoteTransactions)
            Log.d("TransactionRepo", "Synced ${remoteTransactions.size} transactions from Firestore")
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Failed to sync from Firestore", e)
        }
    }

    suspend fun syncToFirestore() {
        try {
            val pending = db.transactionDao().getPendingUploads()
            for (tx in pending) {
                val docRef = transactionsRef.document(tx.id.ifEmpty { transactionsRef.document().id })
                val data = tx.copy(id = docRef.id, needsUpload = false)
                docRef.set(data).await()
                db.transactionDao().insert(data)
            }
            Log.d("TransactionRepo", "Uploaded ${pending.size} local transactions to Firestore")
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Failed to sync to Firestore", e)
        }
    }

    suspend fun ensureSynced() {
        val local = getLocalTransactions()
        if (local.isEmpty()) {
            syncFromFirestore()
        } else {
            syncToFirestore()
        }
    }

    suspend fun addTransaction(tx: Transaction) {
        val toSave = tx.copy(needsUpload = true)
        db.transactionDao().insert(toSave)
    }

    suspend fun deleteAllTransactions() {
        db.transactionDao().deleteAll()
    }
}
