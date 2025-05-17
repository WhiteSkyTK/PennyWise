package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.budget.CategoryLimitAdapter
import com.example.pennywise.data.AppDatabase
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Activitybudget : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)
        ThemeUtils.applyTheme(this)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        val viewModel = ViewModelProvider(this)[BudgetViewModel::class.java]
        var selectedMonth: String = getCurrentYearMonth()
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        val transactionDao = AppDatabase.getDatabase(this).transactionDao()
        val headerManager = HeaderManager(
            this,
            drawerLayout,
            transactionDao,
            lifecycleScope,
            navigationView
        ) { updatedMonthString ->
            val parts = updatedMonthString.split(" ")
            if (parts.size == 2) {
                selectedMonth = "${parts[0]}-${convertMonthNameToNumber(parts[1])}"
                viewModel.loadMonthlyGoal(selectedMonth)
                viewModel.loadCategoryLimitsWithUsage(selectedMonth)
            }
        }
        headerManager.setupDrawerNavigation(navigationView)
        headerManager.setupHeader("Budget")
        val setButton = findViewById<Button>(R.id.setMonthlyBudgetButton)
        val userEmail = intent.getStringExtra("email") ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        viewModel.loadMonthlyGoal(selectedMonth)

        // Check if we need to reload the budget
        if (intent.getBooleanExtra("reload_budget", false)) {
            reloadBudgetAndCategory(selectedMonth, viewModel)
        }

        // Show dialog when button clicked
        setButton.setOnClickListener {
            MonthlyBudgetDialog.show(
                context = this,
                month = selectedMonth,
                existingLimit = null
            ) { categoryLimit ->
                viewModel.saveCategoryLimit(categoryLimit)
                reloadBudgetAndCategory(selectedMonth, viewModel)
            }
        }

        //recyclerview logic
        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        val categoryAdapter = CategoryLimitAdapter(
            items = emptyList(),
            onEdit = { categoryLimit ->
                MonthlyBudgetDialog.show(
                    context = this,
                    month = selectedMonth,
                    existingLimit = categoryLimit
                ) { updatedLimit ->
                    viewModel.saveCategoryLimit(updatedLimit)
                }
            },
            onDelete = { categoryLimit ->
                viewModel.deleteCategoryLimit(categoryLimit)
                reloadBudgetAndCategory(selectedMonth, viewModel)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter

        viewModel.categoryLimits.observe(this) { limits ->
            limits.forEach { limit ->
                Log.d(
                    "CategoryLimitAdapter",
                    "Category: ${limit.category}, Used: ${limit.usedAmount}, Max: ${limit.maxAmount}"
                )
            }
            categoryAdapter.updateData(limits)
        }

        // Load current month's limits
        viewModel.loadCategoryLimitsWithUsage(selectedMonth)

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

        selectedMonth = getCurrentYearMonth()
        BottomNavManager.setupBottomNav(this, R.id.nav_budget)

        //layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.budgetLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    // Method to reload the budget and categories
    private fun reloadBudgetAndCategory(selectedMonth: String, viewModel: BudgetViewModel) {
        // Ensure the viewModel fetches the updated data from the database
        viewModel.loadMonthlyGoal(selectedMonth)  // Reload monthly goal
        viewModel.loadCategoryLimitsWithUsage(selectedMonth)  // Reload category limits
    }
}

//calander logic
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