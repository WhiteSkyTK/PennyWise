package com.example.pennywise

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        //Layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.aboutActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Back Button function
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}