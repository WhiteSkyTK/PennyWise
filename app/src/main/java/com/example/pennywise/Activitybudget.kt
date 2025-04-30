package com.example.pennywise

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import com.example.pennywise.utils.BottomNavManager
import java.util.Locale
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.budget.MonthlyBudgetDialog
import com.example.pennywise.BudgetGoal
import com.example.pennywise.BudgetViewModel
import com.example.pennywise.budget.CategoryLimitAdapter
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter


class Activitybudget : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        val budgetText = findViewById<TextView>(R.id.monthlyBudgetAmount)
        val setButton = findViewById<Button>(R.id.setMonthlyBudgetButton)

        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val viewModel = ViewModelProvider(this)[BudgetViewModel::class.java]
        val userEmail = intent.getStringExtra("email") ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        // Observe and display budget goal
        viewModel.monthlyGoal.observe(this) { goal ->
            if (goal != null) {
                budgetText.text = "R${goal.minAmount} - R${goal.maxAmount}"
            } else {
                budgetText.text = "Not Set"
            }
        }

        viewModel.loadMonthlyGoal(month)

        // Show dialog when button clicked
        setButton.setOnClickListener {
            val currentGoal = viewModel.monthlyGoal.value
            MonthlyBudgetDialog.show(
                context = this,
                month = month,
                existingLimit = null
            ) { categoryLimit ->
                viewModel.saveCategoryLimit(categoryLimit)
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        val categoryAdapter = CategoryLimitAdapter(
            items = emptyList(),
            onEdit = { categoryLimit ->
                MonthlyBudgetDialog.show(
                    context = this,
                    month = month,
                    existingLimit = categoryLimit
                ) { updatedLimit ->
                    viewModel.saveCategoryLimit(updatedLimit)
                }
            },
            onDelete = { categoryLimit ->
                viewModel.deleteCategoryLimit(categoryLimit)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter

        viewModel.categoryLimits.observe(this) { limits ->
            categoryAdapter.updateData(limits)
        }

// Load current month's limits
        viewModel.loadCategoryLimits(month)


        profileInitials.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        startActivity(Intent(this, Activity_Login_Resgister::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }


        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val transactionDao = AppDatabase.getDatabase(this).transactionDao()

        HeaderManager(this, drawerLayout, transactionDao, lifecycleScope) { updatedCalendar ->
            // Optional callback when month changes
        }.setupHeader("Budget")


        BottomNavManager.setupBottomNav(this, R.id.nav_budget)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.budgetLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}