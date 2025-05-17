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
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AddCategory : BaseActivity() {
    // Constants for request codes
    companion object {
        const val REQUEST_CODE_ADD_CATEGORY = 1
        const val REQUEST_CODE_EDIT_CATEGORY = 2
        var shouldRefreshOnResume: Boolean = false
    }

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryDao: CategoryDao
    private lateinit var userEmail: String
    private var selectedMonth: String = getCurrentMonth()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)
        ThemeUtils.applyTheme(this)
        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkTheme) {
            setSystemBars(R.color.black, useLightIcons = false)
        } else {
            setSystemBars(R.color.main_green, useLightIcons = false)
        }


        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val transactionDao = AppDatabase.getDatabase(this).transactionDao()

        val headerManager = HeaderManager(this, drawerLayout, transactionDao, lifecycleScope, navigationView)
        headerManager.setupDrawerNavigation(navigationView)
        headerManager.setupHeader("Report")

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

        findViewById<TextView>(R.id.addCategoryText).setOnClickListener {
            startActivity(Intent(this, Activityaddcategory::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        HeaderManager(this, drawerLayout, transactionDao, lifecycleScope, navigationView) { updatedMonthString ->
            val parts = updatedMonthString.split(" ")
            if (parts.size == 2) {
                selectedMonth = "${parts[0]}-${convertMonthNameToNumber(parts[1])}"
                loadCategories()
            }
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
            emptyMap(),
            onEdit = { category -> editCategory(category) },
            onDelete = { category -> deleteCategory(category) }
        )

        categoryRecyclerView.adapter = categoryAdapter
        loadCategories()
    }

    private fun editCategory(category: Category) {
        val intent = Intent(this, Activityaddcategory::class.java)
        intent.putExtra("category_id", category.id)  // Pass category ID for editing
        startActivityForResult(intent, REQUEST_CODE_EDIT_CATEGORY)
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                categoryDao.deleteCategory(category)
            }
            loadCategories()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check if the request code matches for adding/editing category
        if (requestCode == REQUEST_CODE_ADD_CATEGORY || requestCode == REQUEST_CODE_EDIT_CATEGORY) {
            if (resultCode == RESULT_OK) {
                loadCategories()  // Reload categories when returning from Add/Edit category screen
            }
        }
    }

    fun getCurrentMonth(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    private fun convertMonthNameToNumber(monthName: String): String {
        val month = java.text.SimpleDateFormat("MMM", Locale.ENGLISH).parse(monthName)
        val cal = Calendar.getInstance()
        cal.time = month!!
        return String.format("%02d", cal.get(Calendar.MONTH) + 1)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                categoryDao.getAllCategories()
            }

            // Debug all transactions
            val debugTransactions = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@AddCategory).transactionDao().getAllTransactionsDebug()
            }
            for (tx in debugTransactions) {
                Log.d("TransactionsDebug", "id=${tx.id}, email=${tx.userEmail}, type=${tx.type}, category=${tx.category}, date=${tx.date}, amount=${tx.amount}")
            }

            // Fetch usage totals (concurrently)
            val usageResults = categories.map { category ->
                async(Dispatchers.IO) {
                    val total = categoryDao.getTotalUsedAmountForCategory(category.name, selectedMonth)
                    category.name to total
                }
            }.awaitAll().toMap()

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

            // Update the adapter with categories and usage totals
            categoryAdapter.updateData(categories)
            categoryAdapter.updateTotals(usageResults)
        }
    }
    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            loadCategories()
            shouldRefreshOnResume = false
        }
    }
}