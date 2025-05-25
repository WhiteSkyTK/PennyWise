package com.example.pennywise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlinx.coroutines.tasks.await

class LoadActivity : BaseActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true // <-- This is the key part!
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
        supportActionBar?.hide()

        val logoImageView = findViewById<ImageView>(R.id.imageView17)
        val lottieView = findViewById<LottieAnimationView>(R.id.lottieView)
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_in)
        val lottieAnim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up)

        logoImageView.startAnimation(logoAnim)
        lottieView.startAnimation(lottieAnim)

        val prefs = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("isFirstTime", true)
        val loggedInUserEmail = prefs.getString("loggedInUserEmail", null)

        Log.d("LoadActivity", "isFirstTime = $isFirstTime, loggedInUserEmail = $loggedInUserEmail")

        // Track login streak if logged in
        if (loggedInUserEmail != null) {
            Log.d("LoadActivity", "Tracking login streak & preloading categories for $loggedInUserEmail")
            lifecycleScope.launch {
                trackLoginStreak(loggedInUserEmail)
                preloadUserCategories(loggedInUserEmail)
            }
        }

        Handler().postDelayed({
            val nextActivity = when {
                isFirstTime -> ActivityWelcome::class.java
                loggedInUserEmail != null -> MainActivity::class.java
                else -> ActivityLoginResgister::class.java
            }

            Log.d("LoadActivity", "Navigating to: ${nextActivity.simpleName}")

            startActivity(Intent(this@LoadActivity, nextActivity))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, 2000)
    }

    private suspend fun trackLoginStreak(email: String) {
        try {
            val streakRef = firestore.collection("loginStreaks").document(email)
            val snapshot = streakRef.get().await()

            val calendar = normalizeToMidnight(Calendar.getInstance())
            val currentDateMillis = calendar.timeInMillis
            val currentYear = calendar.get(Calendar.YEAR)

            if (!snapshot.exists()) {
                Log.d("LoginStreak", "First login for $email, inserting new streak data")
                val newStreak = LoginStreak(email, currentDateMillis, 1, 1)
                streakRef.set(newStreak).await()
            } else {
                val streakData = snapshot.toObject(LoginStreak::class.java)
                if (streakData != null) {
                    val lastLoginCal = normalizeToMidnight(Calendar.getInstance().apply {
                        timeInMillis = streakData.lastLoginDate
                    })

                    val daysBetween = (currentDateMillis - lastLoginCal.timeInMillis) / ONE_DAY_MILLIS
                    val lastLoginYear = lastLoginCal.get(Calendar.YEAR)

                    if (daysBetween == 0L) {
                        Log.d("LoginStreak", "Already logged in today. No update.")
                        return
                    }

                    val newTotal = if (lastLoginYear < currentYear) 1 else streakData.totalLoginDaysThisYear + 1
                    val newStreak = if (daysBetween == 1L) streakData.streak + 1 else 1

                    Log.d("LoginStreak", "Updating $email | daysBetween=$daysBetween | newStreak=$newStreak | newTotal=$newTotal")

                    val updatedStreak = LoginStreak(email, currentDateMillis, newTotal, newStreak)
                    streakRef.set(updatedStreak).await()
                }
            }
        } catch (e: Exception) {
            Log.e("LoginStreak", "Error tracking login streak", e)
        }
    }

    private suspend fun preloadUserCategories(email: String) {
        try {
            val categoriesCollection = firestore.collection("categories")
            val userCategoriesQuery = categoriesCollection.whereEqualTo("userEmail", email).get().await()
            val hasUserCategories = !userCategoriesQuery.isEmpty

            if (!hasUserCategories) {
                Log.d("CategoryUpload", "No categories found for $email, preloading now.")

                val batch = firestore.batch()
                for (category in PreloadedCategories.defaultCategories) {
                    val newDocRef = categoriesCollection.document()
                    val categoryMap = mapOf(
                        "name" to category.name,
                        "type" to category.type,
                        "categoryIndex" to category.categoryIndex,
                        "userEmail" to email
                    )
                    batch.set(newDocRef, categoryMap)
                }
                batch.commit().await()
                Log.d("CategoryUpload", "Preloaded categories for $email.")
            } else {
                Log.d("CategoryUpload", "Categories already exist for $email, skipping preload.")
            }
        } catch (e: Exception) {
            Log.e("CategoryUpload", "Error preloading categories for $email", e)
        }
    }


    private val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000
    private fun normalizeToMidnight(calendar: Calendar): Calendar {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
}