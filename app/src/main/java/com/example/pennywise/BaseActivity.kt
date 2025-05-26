package com.example.pennywise

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNav(selectedItemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = selectedItemId
        bottomNav.menu.findItem(R.id.nav_add).isVisible = false // Optional: hide center icon

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_transaction -> {
                    if (selectedItemId != R.id.nav_transaction) {
                        startActivity(Intent(this, MainActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_report -> {
                    if (selectedItemId != R.id.nav_report) {
                        startActivity(Intent(this, ReportActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_budget -> {
                    if (selectedItemId != R.id.nav_budget) {
                        startActivity(Intent(this, Activitybudget::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_category -> {
                    if (selectedItemId != R.id.nav_category) {
                        startActivity(Intent(this, AddCategory::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun setSystemBars(colorResId: Int?, useLightIcons: Boolean = false, transparent: Boolean = false) {
        val window = window
        val decorView = window.decorView

        if (transparent) {
            // Make the status bar transparent
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            // Required flags to draw behind bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION")
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        } else {
            // Use solid color
            colorResId?.let {
                window.statusBarColor = ContextCompat.getColor(this, it)
                window.navigationBarColor = ContextCompat.getColor(this, it)
            }
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }

        // Set icon contrast (false = white icons)
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller?.isAppearanceLightStatusBars = !useLightIcons
        controller?.isAppearanceLightNavigationBars = !useLightIcons
    }
}

open class StatusActivity : AppCompatActivity() {
}