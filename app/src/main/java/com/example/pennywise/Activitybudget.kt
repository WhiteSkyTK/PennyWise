package com.example.pennywise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.budget.CategoryLimitAdapter
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

    private val addEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Check if our specific "needs_refresh" extra is present and true
            if (data?.getBooleanExtra("needs_refresh", false) == true) {
                val transactionMonth = data.getStringExtra("transaction_added_month")
                Log.d("ActivityBudget", "Returned from AddEntry. Transaction added for month: $transactionMonth. Current selectedMonth: $selectedMonth")

                // If a transaction was added for the currently selected month,
                // calling changeSelectedMonth (which calls loadCategoryLimitsWithUsage)
                // will ensure listeners are correctly set up or re-established for fresh data.
                // Even if the LiveData *should* update automatically, this provides an explicit refresh.
                if (transactionMonth != null && transactionMonth == selectedMonth) {
                    Log.d("ActivityBudget", "Transaction was for current month ($selectedMonth). Forcing ViewModel refresh for this month.")
                    viewModel.changeSelectedMonth(selectedMonth) // This should reload both goal and limits
                } else if (transactionMonth != null) {
                    // Optional: If the transaction was for a *different* month, and you want
                    // ActivityBudget to switch to display that month.
                    // If you enable this, make sure your HeaderManager can also be updated.
                    /*
                    Log.d("ActivityBudget", "Transaction was for a different month ($transactionMonth). Switching view to $transactionMonth.")
                    selectedMonth = transactionMonth
                    // You'd need a way to tell HeaderManager to update its displayed month text
                    // headerManager.updateDisplayedMonth(selectedMonth) // This method needs to be created in HeaderManager
                    viewModel.changeSelectedMonth(selectedMonth)
                    */
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

        viewModel.loadMonthlyGoal(selectedMonth)

        // Check if we need to reload the budget
        /*
        if (intent.getBooleanExtra("reload_budget", false)) {
            reloadBudgetAndCategory(selectedMonth, viewModel)
            intent.removeExtra("reload_budget")
        } else {
            viewModel.loadMonthlyGoal(selectedMonth)
            viewModel.loadCategoryLimitsWithUsage(selectedMonth)
        }
         */

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

        //recyclerview logic
        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        categoryAdapter = CategoryLimitAdapter( // Initialize class member
            items = emptyList(),
            onEdit = { categoryLimit ->
                MonthlyBudgetDialog.show(
                    context = this,
                    month = selectedMonth, // Use current selectedMonth
                    existingLimit = categoryLimit
                ) { updatedLimit ->
                    // Ensure the updatedLimit has the correct month
                    val limitToSave = updatedLimit.copy(month = selectedMonth)
                    viewModel.saveCategoryLimit(limitToSave)
                }
            },
            onDelete = { categoryLimit ->
                viewModel.deleteCategoryLimit(categoryLimit)
                // No need to call reloadBudgetAndCategory, listener will update
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter

        viewModel.categoryLimits.observe(this) { limits ->
            Log.d("ActivityBudget", "CategoryLimits LiveData updated. Count: ${limits.size} for month $selectedMonth")
            limits.forEach { limit ->
                Log.d(
                    "ActivityBudget_Observer",
                    "Limit: ${limit.category} (${limit.categoryId}), Used: ${limit.usedAmount}, Max: ${limit.maxAmount}, Month: ${limit.month}"
                )
            }
            categoryAdapter.updateData(limits ?: emptyList())
        }

        viewModel.monthlyGoal.observe(this) { goal ->
            Log.d("ActivityBudget", "MonthlyGoal LiveData updated for month $selectedMonth: $goal")
            // TODO: Update your UI with the monthly goal data
            // e.g., findViewById<TextView>(R.id.monthlyGoalAmountTextView).text = goal?.targetAmount?.toString() ?: "Set Goal"
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

    override fun onResume() {
        super.onResume()
        // It's often good practice to ensure data is fresh when resuming,
        // especially if it could have been changed by another activity not launched for result.
        // However, if using listeners and ActivityResult, this might be redundant.
        // Test carefully. If you see stale data, uncommenting this might be a quick fix,
        // but investigate why listeners aren't catching up first.
        // Log.d("ActivityBudget_Lifecycle", "onResume - Refreshing data for selected month: $selectedMonth")
        // viewModel.changeSelectedMonth(selectedMonth)
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