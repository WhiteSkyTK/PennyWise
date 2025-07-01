package com.example.pennywise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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
import android.view.View
import android.view.ViewAnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlin.math.hypot


class ReportActivity : BaseActivity() {

    companion object {
        var shouldRefreshCharts = false
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: WormDotsIndicator
    private lateinit var chartAdapter: ChartAdapter

    private var selectedMonth: String = getCurrentYearMonth() // Default to current
    private val firestore = FirebaseFirestore.getInstance()

    private val addEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("ReportActivity", "Returned from AddEntry via FAB. Refreshing charts.")
            fetchDataAndUpdateCharts() // Call your existing method to refresh chart data
        } else {
            Log.d("ReportActivity", "Returned from AddEntry via FAB with result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reveal animation if triggered with reveal_x & reveal_y
        val revealX = intent.getIntExtra("reveal_x", -1)
        val revealY = intent.getIntExtra("reveal_y", -1)
        if (revealX != -1 && revealY != -1) {
            val decor = window.decorView
            decor.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                    v.removeOnLayoutChangeListener(this)
                    val finalRadius = hypot(decor.width.toDouble(), decor.height.toDouble()).toFloat()
                    val anim = ViewAnimationUtils.createCircularReveal(decor, revealX, revealY, 0f, finalRadius)
                    decor.visibility = View.VISIBLE
                    anim.duration = 350
                    anim.start()
                }
            })
            window.decorView.visibility = View.INVISIBLE
        }
        setContentView(R.layout.activity_report)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

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
        headerManager.setupDrawerNavigation(navigationView) {
            val view = findViewById<View>(R.id.nav_theme) ?: window.decorView
            val x = (view.x + view.width / 2).toInt()
            val y = (view.y + view.height / 2).toInt()
            TransitionUtil.animateThemeChangeWithReveal(this, x, y)
        }
        headerManager.setupHeader("Report")


        // Profile menu
        val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        val userEmail = intent.getStringExtra("email")
            ?: sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        Log.d("ReportActivity", "Resolved userEmail: $userEmail")

        BottomNavManager.setupBottomNav(this, R.id.nav_report) { fabView ->
            Log.d("ReportActivity_FAB", "FAB clicked, launching AddEntry.")
            val intent = Intent(this@ReportActivity, Activityaddentry::class.java)
            // Pass the current report's month
            intent.putExtra("default_month_year", selectedMonth)
            addEntryLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

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
