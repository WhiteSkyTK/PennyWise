package com.example.pennywise

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.jakewharton.threetenabp.AndroidThreeTen
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener

class PennywiseApplication : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    // CoroutineScope to handle background sync - KEEP THIS
    private val appScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize AdMob SDK FIRST - It's good practice to initialize ads early.
        MobileAds.initialize(this) { initializationStatus ->
            Log.d(
                "PennywiseApplication",
                "Mobile Ads SDK Initialized. Status: $initializationStatus"
            )
            // You can iterate through adapter statuses if needed for debugging:
            // val statusMap = initializationStatus.adapterStatusMap
            // for (adapterClass in statusMap.keys) {
            //     val status = statusMap[adapterClass]
            //     Log.d("PennywiseApplication", "Adapter: $adapterClass, State: ${status?.initializationState}, Description: ${status?.description}")
            // }
        }

        // Initialize AppOpenAdManager and register lifecycle observers for it
        appOpenAdManager = AppOpenAdManager(this)
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)

        // 1. Enable Firestore offline persistence
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
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
        } else {
            Log.d("PennywiseApplication", "No user logged in. Skipping sync.")
        }
    }
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onMoveToForeground() {
            // Show the ad (if available) when the app moves to foreground.
            currentActivity?.let {
                // Optional: Add a check here if you don't want app open ads on certain activities
                // For example, if 'it' is an instance of an activity showing another ad.
                // if (it !is YourAdDisplayingActivity && it !is AdActivity) {
                Log.d(
                    "PennywiseApplication",
                    "App moved to foreground. Attempting to show App Open Ad on: ${it.javaClass.simpleName}"
                )
                appOpenAdManager.showAdIfAvailable(it)
                // }
            }
        }

        // --- ActivityLifecycleCallbacks methods ---
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - Created")
        }

        override fun onActivityStarted(activity: Activity) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - Started")
            // An ad activity is started when an ad is showing (e.g. AdActivity from Google Ads SDK).
            // Don't show an app open ad if one is already visible or being launched.
            if (!appOpenAdManager.isShowingAd) {
                currentActivity = activity
                Log.d(
                    "PennywiseApplication",
                    "Current activity set to: ${activity.javaClass.simpleName}"
                )
            }
        }

        override fun onActivityResumed(activity: Activity) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - Resumed")
            // If you need to specifically update currentActivity on resume:
            // if (!appOpenAdManager.isShowingAd) {
            //     currentActivity = activity
            // }
        }

        override fun onActivityPaused(activity: Activity) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - Paused")
        }

        override fun onActivityStopped(activity: Activity) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - Stopped")
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - SaveInstanceState")
        }

        override fun onActivityDestroyed(activity: Activity) {
            Log.v("ActivityLifecycle", "${activity.javaClass.simpleName} - Destroyed")
            if (currentActivity == activity) {
                currentActivity = null // Clear reference if the current activity is destroyed
            }
        }
}