package com.example.pennywise.utils

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
                        activity.startActivity(Intent(activity, MainActivity::class.java))
                    true
                }

                R.id.nav_report -> {
                    if (selectedItemId != R.id.nav_report)
                        activity.startActivity(Intent(activity, ReportActivity::class.java))
                    true
                }

                R.id.nav_budget -> {
                    if (selectedItemId != R.id.nav_budget)
                        activity.startActivity(Intent(activity, Activitybudget::class.java))
                    true
                }

                R.id.nav_category -> {
                    if (selectedItemId != R.id.nav_category)
                        activity.startActivity(Intent(activity, Add_Category::class.java))
                    true
                }

                else -> false
            }
        }

        fab.setOnClickListener {
            activity.startActivity(Intent(activity, activity_add_entry::class.java))
        }
    }
}