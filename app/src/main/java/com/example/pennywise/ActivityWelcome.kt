package com.example.pennywise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ActivityWelcome : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkTheme) {
            setSystemBars(R.color.black, useLightIcons = false)
        } else {
            setSystemBars(null, useLightIcons = false, transparent = true)
        }

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Animate each shape
        val floatAnim = AnimationUtils.loadAnimation(this, R.anim.float_up_down)

        // Assign animations to image views
        findViewById<ImageView>(R.id.imageView9).startAnimation(floatAnim)    // mid_circle
        findViewById<ImageView>(R.id.imageView8).startAnimation(floatAnim)    // small_square
        findViewById<ImageView>(R.id.imageView3).startAnimation(floatAnim)     // 2x mini_circle
        findViewById<ImageView>(R.id.imageView10).startAnimation(floatAnim)    // mini_square
        findViewById<ImageView>(R.id.imageView11).startAnimation(floatAnim)    // mini_circle
        findViewById<ImageView>(R.id.imageView7).startAnimation(floatAnim)     // big_circle

        // Underline the "Get Started" text
        val getStartedBtn = findViewById<TextView>(R.id.getStartedBtn)
        val content = SpannableString("Get Started")
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        getStartedBtn.text = content

        // Navigation
        getStartedBtn.setOnClickListener {
            // Mark that the app has been opened at least once
            val prefs = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putBoolean("isFirstTime", false)
                apply()
            }
            val intent = Intent(this, ActivityLoginResgister::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}