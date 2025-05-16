package com.example.pennywise

import android.app.DatePickerDialog
import android.content.*
import android.os.Bundle
import android.util.Log
import android.animation.*
import android.view.View
import android.view.ViewGroup
import com.example.pennywise.ThemeUtils
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
    //decleartion
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
        ThemeUtils.applyTheme(this)
        setContentView(R.layout.activity_main)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        //Set today
        val todayDateTextView = findViewById<TextView>(R.id.todayDate)
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        todayDateTextView.text = currentDate

        // Initialize userEmail here
        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"

        //set layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //setup dao and adaptors
        categoryDao = AppDatabase.getDatabase(this).categoryDao()
        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        BottomNavManager.setupBottomNav(this, R.id.nav_transaction)

        drawerLayout = findViewById(R.id.drawerLayout)
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)

        //set users initials
        val userEmail = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        findViewById<ImageView>(R.id.ic_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        //setup navigations
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
                    // Toggle the theme when the theme menu item is selected
                    animateThemeChange()
                    true
                }
                else -> false
            }
        }

        //profile menu functionality
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
                        val intent = Intent(this, Activity_Login_Resgister::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

    //reload logic
    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    //setup the calender
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

    //update calender
    private fun updateCalendarText() {
        val dateFormat = SimpleDateFormat("yyyy MMM", Locale.getDefault())
        calendarText.text = dateFormat.format(currentCalendar.time)
    }

    //date logic
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

    //loading transaction
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

            val incomeView = findViewById<TextView>(R.id.incomeAmount)
            val expenseView = findViewById<TextView>(R.id.expenseAmount)
            val balanceView = findViewById<TextView>(R.id.balanceAmount)

            animateCount(incomeView, 0.0, abs(totalIncome))
            animateCount(expenseView, 0.0, abs(totalExpense))
            animateCount(balanceView, 0.0, totalBalance)

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
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun gameAchieve() {
        val intent = Intent(this, GamificationActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openSupport() {
        val intent = Intent(this, FeedbackActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private var isAnimatingThemeChange = false

    private fun animateThemeChange() {
        if (isAnimatingThemeChange) return
        isAnimatingThemeChange = true

        val rootView = findViewById<View>(R.id.mainLayout)
        val screenshot = rootView.drawToBitmap()

        val overlay = ImageView(this).apply {
            setImageBitmap(screenshot)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        (rootView.parent as ViewGroup).addView(overlay)

        rootView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                ThemeUtils.toggleTheme(this)
                recreate()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                isAnimatingThemeChange = false
            }
            .start()
    }

    private fun animateCount(view: TextView, from: Double, to: Double) {
        val duration = 1000L
        val animator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat())
        animator.duration = duration
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            view.text = "R%.2f".format(value)
        }
        animator.start()
    }
}