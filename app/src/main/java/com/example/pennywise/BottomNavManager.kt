package com.example.pennywise

import android.app.Activity
import android.content.Intent
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

object BottomNavManager {

    //bottom navigation logic
    fun setupBottomNav(activity: Activity, selectedItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
        val fab = activity.findViewById<FloatingActionButton>(R.id.fab)

        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            val selectedView = bottomNav.findViewById<View>(item.itemId)
            val intent = when (item.itemId) {
                R.id.nav_transaction -> {
                    if (selectedItemId == R.id.nav_transaction) return@setOnItemSelectedListener true
                    Intent(activity, MainActivity::class.java)
                }
                R.id.nav_report -> {
                    if (selectedItemId == R.id.nav_report) return@setOnItemSelectedListener true
                    Intent(activity, ReportActivity::class.java)
                }
                R.id.nav_budget -> {
                    if (selectedItemId == R.id.nav_budget) return@setOnItemSelectedListener true
                    Intent(activity, Activitybudget::class.java)
                }
                R.id.nav_category -> {
                    if (selectedItemId == R.id.nav_category) return@setOnItemSelectedListener true
                    Intent(activity, AddCategory::class.java)
                }
                else -> return@setOnItemSelectedListener false
            }

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("from_nav", true)

            TransitionUtil.startCircularRevealTransition(activity, intent, selectedView)
            true
        }

        fab.setOnClickListener {
            val intent = Intent(activity, Activityaddentry::class.java)
            // Optional: decide if you want this to clear stack too
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            TransitionUtil.startCircularRevealTransition(activity, intent, fab)
        }
    }
}
