package com.example.pennywise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch

class LoadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load)
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
            Log.d("LoadActivity", "Tracking login streak for $loggedInUserEmail")
            trackLoginStreak(loggedInUserEmail)
        }

        Handler().postDelayed({
            val nextActivity = when {
                isFirstTime -> Activity_Welcome::class.java
                loggedInUserEmail != null -> MainActivity::class.java
                else -> Activity_Login_Resgister::class.java
            }

            Log.d("LoadActivity", "Navigating to: ${nextActivity.simpleName}")

            startActivity(Intent(this@LoadActivity, nextActivity))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }, 2000)
    }

    private fun trackLoginStreak(email: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val streakDao = db.loginStreakDao()

            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val currentDateMillis = calendar.timeInMillis
            val currentYear = calendar.get(java.util.Calendar.YEAR)

            val streakData = streakDao.getStreak(email)
            if (streakData == null) {
                Log.d("LoginStreak", "First login for $email, inserting new streak data")
                streakDao.insertOrUpdate(LoginStreak(email, currentDateMillis, 1, 1))
            } else {
                val lastLoginCal = java.util.Calendar.getInstance().apply {
                    timeInMillis = streakData.lastLoginDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }

                val daysBetween = (currentDateMillis - lastLoginCal.timeInMillis) / (1000 * 60 * 60 * 24)
                val lastLoginYear = lastLoginCal.get(java.util.Calendar.YEAR)
                val newTotal = if (lastLoginYear < currentYear) 1
                else streakData.totalLoginDaysThisYear + if (daysBetween >= 1) 1 else 0
                val newStreak = when {
                    daysBetween == 1L -> streakData.streak + 1
                    daysBetween > 1L -> 1
                    else -> streakData.streak
                }

                Log.d("LoginStreak", "Updating $email | daysBetween=$daysBetween | newStreak=$newStreak | newTotal=$newTotal")
                streakDao.insertOrUpdate(LoginStreak(email, currentDateMillis, newTotal, newStreak))
            }
        }
    }
}
