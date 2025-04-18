package com.example.pennywise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

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

        Handler().postDelayed({
            val nextActivity = when {
                isFirstTime -> {
                    // First time → Welcome screen
                    Activity_Welcome::class.java
                }
                loggedInUserEmail != null -> {
                    // Logged in → Home screen
                    MainActivity::class.java
                }
                else -> {
                    // Not first time, not logged in → Login/Register screen
                    Activity_Login_Resgister::class.java
                }
            }

            startActivity(Intent(this@LoadActivity, nextActivity))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 4000)
    }
}
