package com.example.pennywise

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pennywise.databinding.ActivityAboutBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
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

        // If using ViewBinding
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val versionName = BuildConfig.VERSION_NAME
        binding.versionNumber.text = "Version $versionName"

        //Back Button function
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}