package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.app.DatePickerDialog
import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.utils.BottomNavManager
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.navigation.NavigationView

class MainActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarText: TextView
    private var currentCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionRecyclerView)

        // Sample data to test
        val groupedItems = listOf(
            TransactionItem.Header("April 2025"),
            TransactionItem.Entry(Transaction("Income", "Freelance Job", "Logo Design", 1200.0, "2025-04-17", "14:00", null)),
            TransactionItem.Entry(Transaction("Expense", "Groceries", "Pick n Pay", 450.0, "2025-04-17", "16:00", null)),
            TransactionItem.Header("March 2025"),
            TransactionItem.Entry(Transaction("Other", "Gift", "Birthday gift", 300.0, "2025-03-29", "12:00", null))
        )

        val adapter = TransactionAdapter(groupedItems)
        transactionRecyclerView.adapter = adapter
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)


        BottomNavManager.setupBottomNav(this, R.id.nav_transaction)

        drawerLayout = findViewById(R.id.drawerLayout)

        val menuIcon: ImageView = findViewById(R.id.ic_menu)
        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())

        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)

        }

        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_about -> {
                    showAppVersion()
                    true
                }
                // Add other cases like nav_profile, nav_currency, etc. here if needed
                else -> false
            }
        }

        profileInitials.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        // Clear the login state from shared preferences
                        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            remove("loggedInUserEmail") // Or clear() to remove everything
                            apply()
                        }

                        // Redirect to login/register screen
                        startActivity(Intent(this, Activity_Login_Resgister::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }
}
