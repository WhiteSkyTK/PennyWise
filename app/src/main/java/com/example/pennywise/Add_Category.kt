package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.data.AppDatabase
import com.example.pennywise.utils.BottomNavManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class Add_Category : BaseActivity() {

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryDao: CategoryDao
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        // Set up user email
        userEmail = intent.getStringExtra("email") ?: "user@example.com"

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
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        findViewById<TextView>(R.id.addCategoryText).setOnClickListener {
            startActivity(Intent(this, activity_add_category::class.java))
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val transactionDao = AppDatabase.getDatabase(this).transactionDao()

        HeaderManager(this, drawerLayout, transactionDao, lifecycleScope) { updatedCalendar ->
            // Optional callback when month changes
        }.setupHeader("Category")

        BottomNavManager.setupBottomNav(this, R.id.nav_category)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize RecyclerView
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize DAO
        categoryDao = AppDatabase.getDatabase(this).categoryDao()

        // Initialize Adapter with empty list
        categoryAdapter = CategoryAdapter(
            emptyList(),
            onEdit = { category -> editCategory(category) },
            onDelete = { category -> deleteCategory(category) }
        )

        categoryRecyclerView.adapter = categoryAdapter

        loadCategories()
    }

    private fun editCategory(category: Category) {
        val intent = Intent(this, activity_add_category::class.java)
        intent.putExtra("category_id", category.id) // you must have an ID field in Category entity
        startActivity(intent)
        loadCategories()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                categoryDao.deleteCategory(category)
            }
            loadCategories()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                categoryDao.getAllCategories()
            }

            // Debug all transactions
            val debugTransactions = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@Add_Category).transactionDao().getAllTransactionsDebug()
            }
            for (tx in debugTransactions) {
                Log.d("TransactionsDebug", "id=${tx.id}, email=${tx.userEmail}, type=${tx.type}, category=${tx.category}, date=${tx.date}, amount=${tx.amount}")
            }

            val transactionDao = AppDatabase.getDatabase(this@Add_Category).transactionDao()
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            val categoryUsageMap = withContext(Dispatchers.IO) {
                val totals = transactionDao.getUsedAmountsByCategory(startOfMonth, endOfMonth, userEmail)
                Log.d("CategoryTotals", "Fetched: $totals")
                Log.d("CategoryTotals", "Fetched: ${totals.map { "${it.category}: ${it.total}" }}")
                Log.d("DateRange", "startOfMonth: $startOfMonth (${Date(startOfMonth)})")
                Log.d("DateRange", "endOfMonth: $endOfMonth (${Date(endOfMonth)})")
                totals.associate { it.category to it.total }
            }

            categoryAdapter.updateData(categories)
            categoryAdapter.updateTotals(categoryUsageMap)
        }
    }
}
