package com.example.pennywise

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout

class Category : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        HeaderManager(this, drawerLayout) { updatedCalendar ->
            // Optional callback when month changes
        }.setupHeader("Report")

        setupBottomNav(R.id.nav_category)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}