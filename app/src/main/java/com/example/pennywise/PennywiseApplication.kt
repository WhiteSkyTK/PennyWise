package com.example.pennywise

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PennywiseApplication : Application() {

    // CoroutineScope to handle background sync
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 1. Enable Firestore offline persistence
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        db.firestoreSettings = settings

        // 2. Initialize Room database
        val database = AppDatabase.getInstance(applicationContext)

        // 3. Initialize theme and time lib
        ThemeUtils.applyTheme(this)
        AndroidThreeTen.init(this)

        Log.d("PennywiseApplication", "Application started")

        // 4. Perform auto-sync if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            Log.d("PennywiseApplication", "User $userId is logged in. Starting auto-sync.")

            val repository = TransactionRepository(database, userId)
            val syncManager = SyncManager(repository)

            appScope.launch {
                syncManager.syncTransactionsIfNeeded()
                Log.d("PennywiseApplication", "Auto-sync complete.")
            }
        } else {
            Log.d("PennywiseApplication", "No user logged in. Skipping sync.")
        }
    }
}
