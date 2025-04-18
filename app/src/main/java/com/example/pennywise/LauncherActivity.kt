package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("pennywise_prefs", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("first_time", true)
        val isLoggedIn = prefs.getBoolean("logged_in", false)

        if (isFirstTime) {
            startActivity(Intent(this, Activity_Welcome::class.java))
            prefs.edit().putBoolean("first_time", false).apply()
        } else if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, Activity_Login_Resgister::class.java))
        }

        finish()
    }
}
