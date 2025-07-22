package com.tk.pennywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AlphaAnimation // Import AlphaAnimation
import android.view.animation.Animation
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.hypot

class ReportActivity : BaseActivity() {

    companion object {
        var shouldRefreshCharts = false
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: WormDotsIndicator
    private lateinit var chartAdapter: ChartAdapter
    private lateinit var loadingAnimationView: LottieAnimationView // Added Lottie View

    private var selectedMonth: String = getCurrentYearMonth() // Default to current
    private val firestore = FirebaseFirestore.getInstance()

    // Keep track of which chart positions have been animated
    private val animatedPositions = mutableSetOf<Int>()

    private val addEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("ReportActivity", "Returned from AddEntry via FAB. Refreshing charts.")
            animatedPositions.clear() // Clear animated positions on refresh
            fetchDataAndUpdateCharts()
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

        // Initialize Views
        viewPager = findViewById(R.id.viewPager)
        dotsIndicator = findViewById(R.id.dots_indicator)
        loadingAnimationView = findViewById(R.id.reportLoadingView) // Initialize Lottie

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

        // Initial setup for ViewPager to handle animation logic
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Request animation for the newly selected page if not already animated
                (viewPager.adapter as? ChartAdapter)?.requestAnimateChart(position)
            }
        })

        // Fetch initial data
        fetchDataAndUpdateCharts()
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshCharts) {
            animatedPositions.clear() // Clear animated positions on refresh
            fetchDataAndUpdateCharts()
            shouldRefreshCharts = false
        }
    }

    // Helper function to manage loading animation visibility
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingAnimationView.visibility = View.VISIBLE
            loadingAnimationView.playAnimation()
            viewPager.visibility = View.INVISIBLE // Keep ViewPager invisible while loading
            dotsIndicator.visibility = View.INVISIBLE
        } else {
            loadingAnimationView.visibility = View.GONE
            loadingAnimationView.cancelAnimation()
            // ViewPager will be made visible with animation in fetchDataAndUpdateCharts
        }
    }

    private fun fetchDataAndUpdateCharts() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val authUser = FirebaseAuth.getInstance().currentUser
                val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
                val userId = authUser?.uid ?: sharedPref.getString("loggedInUserId", "") ?: ""

                Log.d("ReportActivity", "Resolved userId from FirebaseAuth: ${authUser?.uid}")
                Log.d("ReportActivity", "Fallback userId from SharedPreferences: $userId")

                val startDate = getStartOfMonthMillis(selectedMonth)
                val endDate = getEndOfMonthMillis(selectedMonth)

                Log.d("ReportActivity", "Fetching data for userId: $userId")
                Log.d("ReportActivity", "Date range: $startDate to $endDate")

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

                        Log.d(
                            "ReportActivity",
                            "Transaction [$docId] -> category=$category, amount=$amount, type=$type, date=$date"
                        )

                        if (category != null && amount != null && type != null && type.lowercase() == "expense") {
                            val signedAmount = amount
                            categoryTotalsMap[category] =
                                (categoryTotalsMap[category] ?: 0.0) + signedAmount
                        } else {
                            Log.w(
                                "ReportActivity",
                                "Skipping invalid transaction [$docId] due to null field(s)"
                            )
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

                    Log.d(
                        "ReportActivity",
                        "Fetched ${categoryLimitsSnapshot.size()} category limit(s)"
                    )

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
                        Log.w(
                            "ReportActivity",
                            "Chart data list is empty — no transactions found or mapped"
                        )
                    }

                    // Pass the animatedPositions set to the adapter
                    chartAdapter = ChartAdapter(this@ReportActivity, chartDataList, animatedPositions)
                    viewPager.adapter = chartAdapter
                    dotsIndicator.setViewPager2(viewPager)
                    viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                // After data is set, make ViewPager visible with fade-in
                val fadeIn = AlphaAnimation(0f, 1f)
                fadeIn.interpolator = android.view.animation.DecelerateInterpolator() // E.g.
                fadeIn.duration = 500 // Duration in milliseconds
                fadeIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        viewPager.visibility = View.VISIBLE // Make it visible before animation starts
                        dotsIndicator.visibility = View.VISIBLE
                    }
                    override fun onAnimationEnd(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                viewPager.startAnimation(fadeIn)


            } catch (e: Exception) {
                Log.e("ReportActivity", "Error fetching data: ${e.message}", e)
                // Optionally show an error message to the user
            } finally {
                showLoading(false) // Hide loading animation in the finally block
            }
        }
    }

    private fun getCurrentYearMonth(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        return "$year-$month"
    }

    private fun convertMonthNameToNumber(monthName: String): String {
        val month = SimpleDateFormat("MMM", Locale.ENGLISH).parse(monthName)
        val cal = Calendar.getInstance()
        cal.time = month!!
        return String.format("%02d", cal.get(Calendar.MONTH) + 1)
    }

    fun getStartOfMonthMillis(monthYear: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(monthYear)
        val cal = Calendar.getInstance()
        cal.time = date!!
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonthMillis(monthYear: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(monthYear)
        val cal = Calendar.getInstance()
        cal.time = date!!
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
