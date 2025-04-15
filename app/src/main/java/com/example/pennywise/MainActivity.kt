package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.app.DatePickerDialog
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarText: TextView
    private var currentCalendar = Calendar.getInstance()

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
        //Calander
        setupCalendarText()
    }

    private fun setupCalendarText() {
        calendarText = findViewById(R.id.calendarText)
        val calendarPrev = findViewById<ImageView>(R.id.calendarPrev)
        val calendarNext = findViewById<ImageView>(R.id.calendarNext)

        updateCalendarText()

        calendarPrev.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarText()
        }

        calendarNext.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarText()
        }

        calendarText.setOnClickListener {
            openDatePicker()
        }
    }

    private fun updateCalendarText() {
        val dateFormat = SimpleDateFormat("yyyy MMM", Locale.getDefault())
        calendarText.text = dateFormat.format(currentCalendar.time)
    }

    private fun openDatePicker() {
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)
        val day = currentCalendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, _ ->
            currentCalendar.set(Calendar.YEAR, selectedYear)
            currentCalendar.set(Calendar.MONTH, selectedMonth)
            updateCalendarText()
        }, year, month, day)

        datePicker.show()
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
