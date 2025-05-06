package com.example.pennywise

import android.app.DatePickerDialog
import android.content.*
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.core.view.*
import androidx.drawerlayout.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.pennywise.data.AppDatabase
import com.example.pennywise.utils.BottomNavManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MainActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarText: TextView
    private var currentCalendar = Calendar.getInstance()
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var categoryDao: CategoryDao
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val todayDateTextView = findViewById<TextView>(R.id.todayDate)
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        todayDateTextView.text = currentDate


        // Initialize userEmail here
        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        categoryDao = AppDatabase.getDatabase(this).categoryDao()

        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        BottomNavManager.setupBottomNav(this, R.id.nav_transaction)

        drawerLayout = findViewById(R.id.drawerLayout)
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)

        val userEmail = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        findViewById<ImageView>(R.id.ic_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_about -> {
                    showAppVersion(); true
                }
                R.id.nav_gamification -> {
                    gameAchieve(); true
                }
                R.id.nav_feedback -> {
                    openSupport(); true
                }
                R.id.nav_profile -> {
                    openProfile(); true
                }
                R.id.nav_theme -> {
                    openSupport(); true
                }
                else -> false
            }
        }

        profileInitials.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                            .edit()
                            .remove("loggedInUserEmail")
                            .apply()
                        startActivity(Intent(this, Activity_Login_Resgister::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        setupCalendarText()
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)
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
            loadTransactions()
        }, year, month, day)

        datePicker.show()
    }

    private fun loadTransactions() {
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)

        lifecycleScope.launch {
            val selectedMonth = String.format("%02d", currentCalendar.get(Calendar.MONTH) + 1)
            val selectedYear = currentCalendar.get(Calendar.YEAR).toString()
            Log.d("MainActivity", "Loading transactions for $userEmail | $selectedMonth-$selectedYear")

            val transactions = transactionDao.getTransactionsByUserAndMonth(userEmail, selectedMonth, selectedYear)
            val groupedItems = mutableListOf<TransactionItem>()
            val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            transactions.groupBy {
                val date = Date(it.date)
                monthFormatter.format(date)
            }.forEach { (month, group) ->
                groupedItems.addAll(group.map { TransactionItem.Entry(it) })
            }

            val totalIncome = transactions.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }
            val totalBalance = totalIncome - totalExpense

            findViewById<TextView>(R.id.incomeAmount).text = "R%.2f".format(abs(totalIncome))
            findViewById<TextView>(R.id.expenseAmount).text = "R%.2f".format(abs(totalExpense))

            val balanceText = if (totalBalance < 0) "-R%.2f".format(abs(totalBalance)) else "R%.2f".format(totalBalance)
            findViewById<TextView>(R.id.balanceAmount).text = balanceText

            // Iterate through transactions to log the formatted dates
            transactions.forEach { transaction ->
                val formattedDate = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(transaction.date))
                Log.d("MainActivity", "Formatted date for transaction: $formattedDate")
            }

            Log.d("MainActivity", "Grouped item count: ${groupedItems.size}")
            groupedItems.forEach {
                Log.d("MainActivity", "Item: $it")
            }

            if (!::transactionAdapter.isInitialized) {
                transactionAdapter = TransactionAdapter(groupedItems)
                transactionRecyclerView.adapter = transactionAdapter
            } else {
                transactionAdapter.updateData(groupedItems)
            }
        }
    }


    private fun showAppVersion() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    private fun gameAchieve() {
        startActivity(Intent(this, GamificationActivity::class.java))
    }

    private fun openSupport() {
        startActivity(Intent(this, FeedbackActivity::class.java))
    }

    private fun openProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }
}
