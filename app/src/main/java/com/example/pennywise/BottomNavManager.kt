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
            when (item.itemId) {
                R.id.nav_transaction -> {
                    if (selectedItemId != R.id.nav_transaction)
                        TransitionUtil.startCircularRevealTransition(activity, Intent(activity, MainActivity::class.java), selectedView)
                    true
                }
                R.id.nav_report -> {
                    if (selectedItemId != R.id.nav_report)
                        TransitionUtil.startCircularRevealTransition(activity, Intent(activity, ReportActivity::class.java), selectedView)
                    true
                }
                R.id.nav_budget -> {
                    if (selectedItemId != R.id.nav_budget)
                        TransitionUtil.startCircularRevealTransition(activity, Intent(activity, Activitybudget::class.java), selectedView)
                    true
                }
                R.id.nav_category -> {
                    if (selectedItemId != R.id.nav_category)
                        TransitionUtil.startCircularRevealTransition(activity, Intent(activity, AddCategory::class.java), selectedView)
                    true
                }
                else -> false
            }
        }

        fab.setOnClickListener {
            val intent = Intent(activity, Activityaddentry::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            TransitionUtil.startCircularRevealTransition(activity, intent, fab)
        }
    }
}