package com.example.pennywise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class LoadActivity : BaseActivity() {
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
        db.firestoreSettings = settings

        setContentView(R.layout.activity_load)
        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkTheme) {
            setSystemBars(R.color.black, useLightIcons = false)
        } else {
            setSystemBars(null, useLightIcons = false, transparent = true)
        }
        enableEdgeToEdge()
        supportActionBar?.hide()

        val logoImageView = findViewById<ImageView>(R.id.imageView17)
        val lottieView = findViewById<LottieAnimationView>(R.id.lottieView)
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_in)
        val lottieAnim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up)

        logoImageView.startAnimation(logoAnim)
        lottieView.startAnimation(lottieAnim)

        val prefs = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("isFirstTime", true)
        val loggedInUserId = prefs.getString("loggedInUserId", null)

        Log.d("LoadActivity", "isFirstTime = $isFirstTime, loggedInUserId = $loggedInUserId")

        // Track login streak if logged in
        lifecycleScope.launch {
            // 1) track login-streak & daily-visit
            val newVisit = loggedInUserId?.let { trackLoginStreak(it) } ?: false

            // 2) preload categories
            loggedInUserId?.let { preloadUserCategories(it) }

            // 3) wait and navigate
            delay(2000)
            val next = when {
                isFirstTime            -> ActivityWelcome::class.java
                loggedInUserId != null -> MainActivity::class.java
                else                   -> ActivityLoginResgister::class.java
            }
            Log.d("LoadActivity", "Navigating to: ${next.simpleName} (newVisit=$newVisit)")
            startActivity(Intent(this@LoadActivity, next))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    private suspend fun trackLoginStreak(userId: String): Boolean {
        var isNewVisitToday = false
        try {
            // ——— 1) record daily visit ———
            val todayCal = normalizeToMidnight(Calendar.getInstance())
            val todayKey = "%04d-%02d-%02d".format(
                todayCal.get(Calendar.YEAR),
                todayCal.get(Calendar.MONTH) + 1,
                todayCal.get(Calendar.DAY_OF_MONTH)
            )
            val visitsDoc = firestore.collection("users")
                .document(userId)
                .collection("visitLogs")
                .document(todayKey)

            if (!visitsDoc.get().await().exists()) {
                visitsDoc.set(mapOf("visited" to true)).await()
                isNewVisitToday = true
                Log.d("LoginStreak", "Logged daily visit for $todayKey")
            } else {
                Log.d("LoginStreak", "Visit already logged for $todayKey")
            }

            // ——— 2) update streak subcollection ———
            val streakRef = firestore.collection("users")
                .document(userId)
                .collection("loginStreaks")
                .document("current")
            val snap = streakRef.get().await()

            val todayMillis = todayCal.timeInMillis
            val todayYear   = todayCal.get(Calendar.YEAR)

            if (!snap.exists()) {
                Log.d("LoginStreak", "First-ever login for $userId, creating streak doc")
                val initial = LoginStreak(
                    userId,
                    lastLoginDate = todayMillis,
                    totalLoginDaysThisYear = 1,
                    streak = 1
                )
                streakRef.set(initial).await()
            } else {
                val data = snap.toObject(LoginStreak::class.java)!!
                val lastCal = normalizeToMidnight(Calendar.getInstance().apply {
                    timeInMillis = data.lastLoginDate
                })
                val daysBetween = (todayMillis - lastCal.timeInMillis) / ONE_DAY_MILLIS

                if (daysBetween == 0L) {
                    Log.d("LoginStreak", "Already logged today; no streak change")
                } else {
                    val newTotal  = if (lastCal.get(Calendar.YEAR) < todayYear)
                        1 else data.totalLoginDaysThisYear + 1
                    val newStreak = if (daysBetween == 1L)
                        data.streak + 1 else 1

                    Log.d("LoginStreak",
                        "Streak update: daysBetween=$daysBetween, newStreak=$newStreak, newTotal=$newTotal"
                    )
                    val updated = LoginStreak(userId, todayMillis, newTotal, newStreak)
                    streakRef.set(updated).await()
                }
            }
        } catch (e: Exception) {
            Log.e("LoginStreak", "Error tracking login streak or visit", e)
        }
        return isNewVisitToday
    }

    private suspend fun preloadUserCategories(userId: String) {
        try {
            val categoriesCollection = firestore.collection("categories")

            // Query categories by userId (not email)
            val userCategoriesQuery = categoriesCollection.whereEqualTo("userId", userId).get().await()
            val hasUserCategories = !userCategoriesQuery.isEmpty

            if (!hasUserCategories) {
                Log.d("CategoryUpload", "No categories found for $userId, preloading now.")

                val batch = firestore.batch()
                for (category in PreloadedCategories.defaultCategories) {
                    val newDocRef = categoriesCollection.document()
                    val categoryMap = mapOf(
                        "name" to category.name,
                        "type" to category.type,
                        "categoryIndex" to category.categoryIndex,
                        "userId" to userId  // Use userId here
                    )
                    batch.set(newDocRef, categoryMap)
                }
                batch.commit().await()
                Log.d("CategoryUpload", "Preloaded categories for $userId.")
            } else {
                Log.d("CategoryUpload", "Categories already exist for $userId, skipping preload.")
            }
        } catch (e: Exception) {
            Log.e("CategoryUpload", "Error preloading categories for $userId", e)
        }
    }

    private val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    private fun normalizeToMidnight(calendar: Calendar): Calendar {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE,      0)
        calendar.set(Calendar.SECOND,      0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
}