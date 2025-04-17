package com.example.pennywise

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GamificationActivity : AppCompatActivity() {

    // Placeholder for your RecyclerView and Progress components
    private lateinit var badgeRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var levelText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gamification)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gamificationLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button logic
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Initialize views
        badgeRecycler = findViewById(R.id.badgeRecycler)
        progressBar = findViewById(R.id.levelProgressBar)
        levelText = findViewById(R.id.levelText)

        // Set up the RecyclerView
        setupBadgeRecycler()

        // Load user progress, level, and achievements
        loadUserGamificationData()
    }

    // 🔧 Placeholder function to set up RecyclerView
    private fun setupBadgeRecycler() {
        badgeRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Placeholder: create dummy badges for now
        /*val badges = listOf(
            Badge("First Claim", "Submitted your first claim", R.drawable.ic_badge_claim),
            Badge("Streak Master", "Logged in 5 days in a row", R.drawable.ic_badge_streak),
            Badge("Verified Pro", "Verified email & profile", R.drawable.ic_badge_verified)
        )
*/
        // Adapter placeholder (create later)
        //val adapter = BadgeAdapter(badges)
        //badgeRecycler.adapter = adapter
    }

    // 🧠 Placeholder function to load gamification data
    private fun loadUserGamificationData() {
        // Example: Set level and progress
        val currentLevel = 3
        val progressPercent = 60

        levelText.text = "Level $currentLevel"
        progressBar.progress = progressPercent

        Log.d("Gamification", "Loaded user level: $currentLevel with progress: $progressPercent%")
    }

    // 💬 Later you can connect to a database or shared preferences to store/retrieve real user data
}
