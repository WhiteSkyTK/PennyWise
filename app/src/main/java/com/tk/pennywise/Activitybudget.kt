package com.tk.pennywise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.text
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.hypot

class Activitybudget : BaseActivity() {
    private val auth = FirebaseAuth.getInstance()
    private lateinit var viewModel: BudgetViewModel // Make viewModel a class member
    private lateinit var categoryAdapter: CategoryLimitAdapter // Make adapter a class member
    private var selectedMonth: String = getCurrentYearMonth() // Initialize selectedMonth

    private lateinit var recyclerView: RecyclerView // Declare RecyclerView
    private lateinit var loadingAnimationView: LottieAnimationView // Declare Lottie

    private val addEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("needs_refresh", false) == true) {
                val transactionMonth = data.getStringExtra("transaction_added_month")
                Log.d("ActivityBudget", "Returned from AddEntry. Transaction added for month: $transactionMonth. Current selectedMonth: $selectedMonth")
                if (transactionMonth != null && transactionMonth == selectedMonth) {
                    Log.d("ActivityBudget", "Transaction was for current month ($selectedMonth). Forcing ViewModel refresh for this month.")
                    // No need to explicitly call showLoading(true) here if LiveData observer handles UI updates
                    viewModel.changeSelectedMonth(selectedMonth)
                } else if (transactionMonth != null) {
                    Log.d("ActivityBudget", "Transaction was for a different month ($transactionMonth). Current view remains on $selectedMonth.")
                }
            } else {
                Log.d("ActivityBudget", "Returned from AddEntry, but no refresh needed or data missing.")
            }
        } else {
            Log.d("ActivityBudget", "Returned from AddEntry with result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

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
        setContentView(R.layout.activity_budget)
        recyclerView = findViewById(R.id.categoryRecyclerView) // Make sure this ID matches your activity_budget.xml
        loadingAnimationView = findViewById(R.id.lottieLoadingViewBudget) // Make sure this ID matches your activity_budget.xml

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()


        this.viewModel = ViewModelProvider(this)[BudgetViewModel::class.java]

        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        Log.d("SelectedMonth", "Current selected month is: $selectedMonth")

        val headerManager = HeaderManager(
            this,
            drawerLayout,
            navigationView
        ) { updatedMonthString ->
            val parts = updatedMonthString.split(" ")
            if (parts.size == 2) {
                val newSelectedMonth = "${parts[0]}-${convertMonthNameToNumber(parts[1])}"
                if (newSelectedMonth != selectedMonth) {
                    selectedMonth = newSelectedMonth
                    Log.d("ActivityBudget", "Month changed by HeaderManager to: $selectedMonth")
                    viewModel.changeSelectedMonth(selectedMonth) // ViewModel handles listener setup
                }
            }
        }
        headerManager.setupDrawerNavigation(navigationView) {
            val view = findViewById<View>(R.id.nav_theme) ?: window.decorView
            val x = (view.x + view.width / 2).toInt()
            val y = (view.y + view.height / 2).toInt()
            TransitionUtil.animateThemeChangeWithReveal(this, x, y)
        }
        headerManager.setupHeader("Budget")

        val setButton = findViewById<Button>(R.id.setMonthlyBudgetButton)
        setButton.text = "Add Budget"

        viewModel.loadMonthlyGoal(selectedMonth)

        // Show dialog when button clicked
        setButton.setOnClickListener {
            MonthlyBudgetDialog.show(
                context = this,
                month = selectedMonth, // Use the current selectedMonth
                existingLimit = null
            ) { categoryLimit ->
                // Ensure the categoryLimit has the correct month before saving
                val limitToSave = categoryLimit.copy(month = selectedMonth)
                viewModel.saveCategoryLimit(limitToSave)
                // No need to call reloadBudgetAndCategory explicitly if listeners are working
            }
        }

        categoryAdapter = CategoryLimitAdapter(
            items = emptyList(),
            onEdit = { categoryLimitFromAdapter ->
                MonthlyBudgetDialog.show(
                    context = this,
                    month = selectedMonth,
                    existingLimit = categoryLimitFromAdapter
                ) { updatedLimitDataFromDialog ->
                    val limitToSave = updatedLimitDataFromDialog.copy(
                    id = categoryLimitFromAdapter.categoryId,
                    month = selectedMonth,
                    userId = categoryLimitFromAdapter.userId
                    )
                    viewModel.saveCategoryLimit(limitToSave)
                }
            },
            onDelete = { categoryLimit ->
                viewModel.deleteCategoryLimit(categoryLimit)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter

        // Observe category limits - ONLY update the adapter here
        viewModel.categoryLimits.observe(this) { limits ->
            Log.d("ActivityBudget", "CategoryLimits LiveData observed. Count: ${limits?.size ?: 0} for month $selectedMonth.")
            // The showLoading(false) will be handled by the isLoading observer
            if (limits != null) {
                categoryAdapter.updateData(limits)
                Log.d("ActivityBudget_Observer", "Data updated with ${limits.size} items.")
            } else {
                categoryAdapter.updateData(emptyList())
                Log.d("ActivityBudget_Observer", "Data updated with empty list (null received).")
            }
        }

        // Observe loading state from ViewModel - THIS controls the loading UI
        viewModel.isLoading.observe(this) { isLoading ->
            Log.d("ActivityBudget", "isLoading LiveData observed: $isLoading")
            showLoading(isLoading) // This will correctly show/hide based on the latest state
        }

        viewModel.monthlyGoal.observe(this) { goal ->
            Log.d("ActivityBudget", "MonthlyGoal LiveData updated for month $selectedMonth: $goal")
            // Update UI with monthly goal (e.g., in the monthlyBudgetCard)
            // val budgetGoalAmountTextView = findViewById<TextView>(R.id.monthlyGoalAmountTextView) // You'd need this TextView in your XML
            // budgetGoalAmountTextView.text = goal?.targetAmount?.toString() ?: "Goal Not Set"
        }

        // Load current month's limits
        viewModel.loadCategoryLimitsWithUsage(selectedMonth)

        BottomNavManager.setupBottomNav(this, R.id.nav_budget) { fabView ->
            val intent = Intent(this@Activitybudget, Activityaddentry::class.java)
            intent.putExtra("default_month_year", selectedMonth)
            addEntryLauncher.launch(intent)
        }

        //layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.budgetLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Method to reload the budget and categories
    private fun reloadBudgetAndCategory(monthToReload: String, currentViewModel: BudgetViewModel) {
        Log.d("ActivityBudget", "Manually calling reloadBudgetAndCategory for month: $monthToReload")
        currentViewModel.changeSelectedMonth(monthToReload)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            Log.d("ActivityBudget", "showLoading: TRUE - Hiding RecyclerView, Showing Lottie")
            recyclerView.visibility = View.GONE // Hide RecyclerView
            loadingAnimationView.visibility = View.VISIBLE
            loadingAnimationView.playAnimation()
        } else {
            Log.d("ActivityBudget", "showLoading: FALSE - Showing RecyclerView, Hiding Lottie")
            recyclerView.visibility = View.VISIBLE // Show RecyclerView
            loadingAnimationView.visibility = View.GONE
            loadingAnimationView.cancelAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("ActivityBudget", "onResume called") // Added a log message

        // --- START OF CHANGES ---
        // Ensure the BottomNavigationView correctly highlights the "Budget" item
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        if (bottomNav.selectedItemId != R.id.nav_budget) {
            bottomNav.selectedItemId = R.id.nav_budget
        }
        // --- END OF CHANGES ---

        // Consider if a refresh is truly needed here or if LiveData from ViewModel handles it.
        // If data might change from other sources without this activity knowing, a refresh might be good.
        // Log.d("ActivityBudget_Lifecycle", "onResume - Refreshing data for selected month: $selectedMonth")
        // viewModel.changeSelectedMonth(selectedMonth) // This will trigger loading state if ViewModel is set up for it
    }

    companion object {
        fun getCurrentYearMonth(): String {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = String.format("%02d", cal.get(Calendar.MONTH) + 1) // Month is 0-indexed
            return "$year-$month" // Format: YYYY-MM
        }

        fun convertMonthNameToNumber(monthName: String): String {
            return try {
                val monthDate = SimpleDateFormat("MMM", Locale.ENGLISH).parse(monthName)
                val cal = Calendar.getInstance()
                monthDate?.let { cal.time = it }
                String.format("%02d", cal.get(Calendar.MONTH) + 1) // Month is 0-indexed
            } catch (e: Exception) {
                Log.e("ActivityBudget", "Error converting month name: $monthName", e)
                String.format("%02d", Calendar.getInstance().get(Calendar.MONTH) + 1) // Default to current month on error
            }
        }
    }
}