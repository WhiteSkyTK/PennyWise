package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup bottom nav and set current selected item
        setupBottomNav(R.id.nav_transaction)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this, activity_add_entry::class.java)
            startActivity(intent)
        }
        drawerLayout = findViewById(R.id.drawerLayout)

        val menuIcon: ImageView = findViewById(R.id.ic_menu)
        val profileIcon: ImageView = findViewById(R.id.profileIcon)

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)

        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val navView: NavigationView = findViewById(R.id.navigationView)
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.nav_theme -> toggleTheme()
                R.id.nav_currency -> changeCurrency()
                R.id.nav_gamification -> startActivity(Intent(this, GamificationActivity::class.java))
                R.id.nav_feedback -> openSupport()
                R.id.nav_about -> showAppVersion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun toggleTheme() {
        // You can implement dark/light theme toggle here
    }

    private fun changeCurrency() {
        // Open a dialog or another activity to choose a currency
    }

    private fun openSupport() {
        // Launch support activity or email intent
    }

    private fun showAppVersion() {
        // Toast or dialog showing app version
    }
}
