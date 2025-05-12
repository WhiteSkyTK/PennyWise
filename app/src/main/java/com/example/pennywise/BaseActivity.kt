package com.example.pennywise

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
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
                        startActivity(Intent(this, Add_Category::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }
}