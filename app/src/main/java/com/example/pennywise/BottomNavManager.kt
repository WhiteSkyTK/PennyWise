package com.example.pennywise

import android.app.Activity
import android.content.Intent
import com.example.pennywise.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

object BottomNavManager {

    //bottom navigation logic
    fun setupBottomNav(activity: Activity, selectedItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
        val fab = activity.findViewById<FloatingActionButton>(R.id.fab)

        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_transaction -> {
                    if (selectedItemId != R.id.nav_transaction)
                        startFadeTransition(activity, Intent(activity, MainActivity::class.java))
                    true
                }
                R.id.nav_report -> {
                    if (selectedItemId != R.id.nav_report)
                        startFadeTransition(activity, Intent(activity, ReportActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    if (selectedItemId != R.id.nav_budget)
                        startFadeTransition(activity, Intent(activity, Activitybudget::class.java))
                    true
                }
                R.id.nav_category -> {
                    if (selectedItemId != R.id.nav_category)
                        startFadeTransition(activity, Intent(activity, AddCategory::class.java))
                    true
                }
                else -> false
            }
        }

        fab.setOnClickListener {
            startFadeTransition(activity, Intent(activity, Activityaddentry::class.java))
        }
    }

    fun startFadeTransition(activity: Activity, targetIntent: Intent) {
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(targetIntent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

}