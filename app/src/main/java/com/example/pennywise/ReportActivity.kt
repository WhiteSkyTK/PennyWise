package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Locale

class ReportActivity : BaseActivity() {

    companion object {
        var shouldRefreshCharts = false
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: WormDotsIndicator
    private lateinit var chartAdapter: ChartAdapter
    private lateinit var saveTransactionLauncher: ActivityResultLauncher<Intent>

    private var selectedMonth: String = getCurrentYearMonth() // Default to current
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true // <-- This is the key part!
        }
        db.firestoreSettings = settings
        setContentView(R.layout.activity_report)
        ThemeUtils.applyTheme(this)

        // Log resolved color from resources
        val statusBarColor = ContextCompat.getColor(this, R.color.main_green)
        Log.d("ReportActivity", "Resolved status bar color (int): $statusBarColor")
        Log.d("ReportActivity", "Resolved status bar color (hex): #${Integer.toHexString(statusBarColor)}")

        supportActionBar?.hide()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // HeaderManager with month selection callback
        val headerManager = HeaderManager(this, drawerLayout, navigationView) { updatedMonthString ->
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
                        startActivity(Intent(this, ActivityLoginResgister::class.java))
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

    override fun onResume() {
        super.onResume()
        if (shouldRefreshCharts) {
            fetchDataAndUpdateCharts()
            shouldRefreshCharts = false
        }
    }

    private fun fetchDataAndUpdateCharts() {
        lifecycleScope.launch {
            val authUser = FirebaseAuth.getInstance().currentUser
            val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
            val userId = authUser?.uid ?: sharedPref.getString("loggedInUserId", "") ?: ""

            Log.d("ReportActivity", "Resolved userId from FirebaseAuth: ${authUser?.uid}")
            Log.d("ReportActivity", "Fallback userId from SharedPreferences: $userId")

            val startDate = getStartOfMonthMillis(selectedMonth)
            val endDate = getEndOfMonthMillis(selectedMonth)

            Log.d("ReportActivity", "Fetching data for userId: $userId")
            Log.d("ReportActivity", "Date range: $startDate to $endDate")

            try {
                // --- Fetch transactions ---
                val transactionsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
                    .get()
                    .await()

                Log.d("ReportActivity", "Fetched ${transactionsSnapshot.size()} transaction(s)")

                val categoryTotalsMap = mutableMapOf<String, Double>()

                for (doc in transactionsSnapshot.documents) {
                    val docId = doc.id
                    val category = doc.getString("category")
                    val amount = doc.getDouble("amount")
                    val type = doc.getString("type")
                    val date = doc.getLong("date")

                    Log.d("ReportActivity", "Transaction [$docId] -> category=$category, amount=$amount, type=$type, date=$date")

                    if (category != null && amount != null && type != null && type.lowercase() == "expense") {
                        val signedAmount = amount
                        categoryTotalsMap[category] = (categoryTotalsMap[category] ?: 0.0) + signedAmount
                    } else {
                        Log.w("ReportActivity", "Skipping invalid transaction [$docId] due to null field(s)")
                    }
                }

                Log.d("ReportActivity", "Aggregated category totals: $categoryTotalsMap")

                // --- Fetch category limits ---
                val categoryLimitsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categoryLimits")
                    .whereEqualTo("month", selectedMonth)
                    .get()
                    .await()

                Log.d("ReportActivity", "Fetched ${categoryLimitsSnapshot.size()} category limit(s)")

                val categoryLimitsMap = categoryLimitsSnapshot.documents.associateBy(
                    { it.getString("category") ?: "" },
                    { it }
                )

                Log.d("ReportActivity", "Category limits map: $categoryLimitsMap")

                // --- Fetch budget goal ---
                val budgetGoalSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("budgetGoals")
                    .whereEqualTo("month", selectedMonth)
                    .get()
                    .await()

                Log.d("ReportActivity", "Fetched ${budgetGoalSnapshot.size()} budget goal(s)")

                val budgetGoalDoc = budgetGoalSnapshot.documents.firstOrNull()
                val budgetMin = budgetGoalDoc?.getDouble("minAmount")?.toFloat() ?: 0f
                val budgetMax = budgetGoalDoc?.getDouble("maxAmount")?.toFloat() ?: 0f

                Log.d("ReportActivity", "Budget goal -> min: $budgetMin, max: $budgetMax")

                // --- Build ChartData list ---
                val chartDataList = categoryTotalsMap.map { (category, total) ->
                    val limitDoc = categoryLimitsMap[category]
                    val minLimit = limitDoc?.getDouble("minAmount")
                    val maxLimit = limitDoc?.getDouble("maxAmount")

                    ChartData(
                        category = category,
                        value = total,
                        min = minLimit,
                        max = maxLimit
                    ).also {
                        Log.d("ReportActivity", "Mapped chart data: $it")
                    }
                }

                if (chartDataList.isEmpty()) {
                    Log.w("ReportActivity", "Chart data list is empty — no transactions found or mapped")
                }

                chartAdapter = ChartAdapter(this@ReportActivity, chartDataList)
                viewPager.adapter = chartAdapter
                dotsIndicator.setViewPager2(viewPager)
                viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

            } catch (e: Exception) {
                Log.e("ReportActivity", "Error fetching data: ${e.message}", e)
            }
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
