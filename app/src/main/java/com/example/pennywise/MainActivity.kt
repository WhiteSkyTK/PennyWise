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
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.data.AppDatabase
import com.example.pennywise.utils.BottomNavManager
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarText: TextView
    private var currentCalendar = Calendar.getInstance()
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionAdapter: TransactionAdapter

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

        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)

        transactionDao = AppDatabase.getDatabase(this).transactionDao()

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
                        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            remove("loggedInUserEmail") // Or clear() to remove everything
                            apply()
                        }
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
    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun setupCalendarText() {
        calendarText = findViewById(R.id.calendarText)
        val calendarPrev = findViewById<ImageView>(R.id.calendarPrev)
        val calendarNext = findViewById<ImageView>(R.id.calendarNext)

        updateCalendarText()

        calendarPrev.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarText()
            loadTransactions()
        }

        calendarNext.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarText()
            loadTransactions()
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
            loadTransactions() //reload
        }, year, month, day)

        datePicker.show()
    }

    private fun loadTransactions() {
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)

        lifecycleScope.launch {
            val userEmail = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                .getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"

            val selectedMonth = String.format("%02d", currentCalendar.get(Calendar.MONTH) + 1)
            val selectedYear = currentCalendar.get(Calendar.YEAR).toString()

            Log.d("MainActivity", "Loading transactions for $userEmail | $selectedMonth-$selectedYear")

            val transactions = transactionDao.getTransactionsByUserAndMonth(userEmail, selectedMonth, selectedYear)

            // Group by month (e.g., "April 2025")
            val groupedItems = mutableListOf<TransactionItem>()
            val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            transactions.groupBy {
                val date = Date(it.date)
                monthFormatter.format(date)
            }.forEach { (month, group) ->
                groupedItems.add(TransactionItem.Header(month))
                groupedItems.addAll(group.map { TransactionItem.Entry(it) })
            }

            val totalIncome = transactions.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }
            val totalBalance = totalIncome - totalExpense

            findViewById<TextView>(R.id.incomeAmount).text = "R%.2f".format(abs(totalIncome))
            findViewById<TextView>(R.id.expenseAmount).text = "R%.2f".format(abs(totalExpense))

            // For balance, let the sign show normally
            val balanceText = if (totalBalance < 0) "-R%.2f".format(abs(totalBalance)) else "R%.2f".format(totalBalance)
            findViewById<TextView>(R.id.balanceAmount).text = balanceText

            if (!::transactionAdapter.isInitialized) {
                transactionAdapter = TransactionAdapter(groupedItems)
                transactionRecyclerView.adapter = transactionAdapter
            } else {
                transactionAdapter.updateData(groupedItems)
            }
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
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }
}
