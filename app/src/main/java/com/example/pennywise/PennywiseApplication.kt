package com.example.pennywise

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.jakewharton.threetenabp.AndroidThreeTen

class PennywiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true // <-- This is the key part!
        }
        db.firestoreSettings = settings
        // Apply the stored theme when the app starts
        ThemeUtils.applyTheme(this)
        AndroidThreeTen.init(this)
        Log.d("PennywiseApplication", "Application started")
    }
}