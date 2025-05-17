package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.pennywise.data.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import kotlinx.coroutines.launch
import java.util.Locale


class ReportActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: WormDotsIndicator
    private lateinit var chartAdapter: ChartAdapter
    private lateinit var transactionDao: TransactionDao

    private var selectedMonth: String = getCurrentYearMonth() // Default to current

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        ThemeUtils.applyTheme(this)

        // Log resolved color from resources
        val statusBarColor = ContextCompat.getColor(this, R.color.main_green)
        Log.d("ReportActivity", "Resolved status bar color (int): $statusBarColor")
        Log.d("ReportActivity", "Resolved status bar color (hex): #${Integer.toHexString(statusBarColor)}")

        supportActionBar?.hide()

        transactionDao = AppDatabase.getDatabase(this).transactionDao()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // HeaderManager with month selection callback
        val headerManager = HeaderManager(this, drawerLayout, transactionDao, lifecycleScope, navigationView) { updatedMonthString ->
            val parts = updatedMonthString.split(" ")
            if (parts.size == 2) {
                selectedMonth = "${parts[0]}-${convertMonthNameToNumber(parts[1])}"
                Log.d("ReportActivity", "Month selected: $selectedMonth")
                fetchDataAndUpdateCharts()
            }
        }
        headerManager.setupDrawerNavigation(navigationView)
        headerManager.setupHeader("Report")

        // Profile menu
        val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        val userEmail = intent.getStringExtra("email")
            ?: sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        Log.d("ReportActivity", "Resolved userEmail: $userEmail")
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials
        profileInitials.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        startActivity(Intent(this, Activity_Login_Resgister::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        BottomNavManager.setupBottomNav(this, R.id.nav_report)

        viewPager = findViewById(R.id.viewPager)
        dotsIndicator = findViewById(R.id.dots_indicator)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewBadgesBtn: MaterialButton = findViewById(R.id.viewBadgesBtn)
        viewBadgesBtn.setOnClickListener {
            val intent = Intent(this, GamificationActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // optional animation
        }

        // Fetch initial data
        fetchDataAndUpdateCharts()
    }

    private fun fetchDataAndUpdateCharts() {
        lifecycleScope.launch {
            val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
            val email = intent.getStringExtra("email")
                ?: sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"

            val startDate = getStartOfMonthMillis(selectedMonth)
            val endDate = getEndOfMonthMillis(selectedMonth)

            Log.d("ReportActivity", "Fetching data for email: $email, startDate: $startDate, endDate: $endDate")

            val categoryTotals = transactionDao.getSpendingByCategoryInRange(email, startDate, endDate)
            val categoryLimitDao = AppDatabase.getDatabase(this@ReportActivity).categoryLimitDao()
            val budgetGoalDao = AppDatabase.getDatabase(this@ReportActivity).budgetGoalDao()
            // Fetch budget goal for this month (this is what you need)
            val budgetGoal = budgetGoalDao.getBudgetGoal(selectedMonth)
            val budgetMin = budgetGoal?.minAmount?.toFloat() ?: 0f
            val budgetMax = budgetGoal?.maxAmount?.toFloat() ?: 0f

            val chartDataList = categoryTotals.map { total ->
                val limit = categoryLimitDao.getCategoryLimit(selectedMonth, total.category)

                val chartData = ChartData(
                    category = total.category,
                    value = total.total,
                    min = limit?.minAmount,
                    max = limit?.maxAmount
                )

                Log.d("ReportActivity", "Mapped chart data: $chartData")
                chartData
            }

            chartAdapter = ChartAdapter(this@ReportActivity, chartDataList)
            viewPager.adapter = chartAdapter
            dotsIndicator.setViewPager2(viewPager)
            viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }
    }

    private fun getCurrentYearMonth(): String {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
        return "$year-$month"
    }

    private fun convertMonthNameToNumber(monthName: String): String {
        val month = java.text.SimpleDateFormat("MMM", Locale.ENGLISH).parse(monthName)
        val cal = java.util.Calendar.getInstance()
        cal.time = month!!
        return String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
    }

    fun getStartOfMonthMillis(monthYear: String): Long {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(monthYear)
        val cal = java.util.Calendar.getInstance()
        cal.time = date!!
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonthMillis(monthYear: String): Long {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(monthYear)
        val cal = java.util.Calendar.getInstance()
        cal.time = date!!
        cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
