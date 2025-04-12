package com.example.pennywise

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

        // Load the splash screen layout
        setContentView(R.layout.activity_load)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        // Reference UI components
        val logoImageView = findViewById<ImageView>(R.id.imageView17)
        val lottieView = findViewById<LottieAnimationView>(R.id.lottieView)

        // Load animation resources
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_in)
        val lottieAnim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up)

        // Start animations
        logoImageView.startAnimation(logoAnim)
        lottieView.startAnimation(lottieAnim)

        // After delay, navigate to login/register screen with a fade transition
        Handler().postDelayed({
            startActivity(Intent(this@LoadActivity, Activity_Welcome::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 4000) // 4 seconds delay
    }
}
