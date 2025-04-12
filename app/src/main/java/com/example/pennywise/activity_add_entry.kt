package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.*

class activity_add_entry : AppCompatActivity() {

    private lateinit var dateButton: Button
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var categorySpinner: Spinner
    private lateinit var addCategoryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_entry)

        // Handle insets for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        dateButton = findViewById(R.id.dateButton)
        typeRadioGroup = findViewById(R.id.typeRadioGroup)
        categorySpinner = findViewById(R.id.categorySpinner)
        addCategoryText = findViewById(R.id.addCategoryText)

        // Add Category Button Logic
        addCategoryText.setOnClickListener {
            startActivity(Intent(this, activity_add_category::class.java))
        }

        // Set default date to today
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        dateButton.text = currentDate

        // Categories
        val expenseCategories = listOf(
            "Accessories", "Alcohol", "Baby Supplies", "Bills", "Books", "Car", "Charity", "Clothes", "Coffee",
            "Delivery", "Dining Out", "Electricity", "Fine", "Fuel", "Gaming", "Gifts", "Groceries", "Gym",
            "Haircut", "Hardware", "Healthcare", "Insurance", "Internet", "Laundry", "Lottery", "Miscellaneous",
            "Movies", "Music", "Parking", "Pet Care", "Phone", "Public Transport", "Rent", "Repairs", "Snacks",
            "Software", "Spa", "Stationery", "Streaming", "Subscription", "Taxi", "Tools", "Toys", "Travel",
            "Tuition", "Water", "WiFi", "Other"
        )

        val incomeCategories = listOf(
            "Affiliate", "Allowance", "Blog", "Bonus", "Cashback", "Coding", "Commission", "Consulting", "Digital Art",
            "Dividends", "Dog Walking", "Donations", "Dropshipping", "eBook", "Event Hosting", "Freelance", "Gift",
            "Grant", "Hair Braiding", "Income", "Interest", "Investment", "Lottery", "Online Courses", "Other",
            "Part-time Job", "Passive Income", "Pension", "Prize", "Profit", "Refund", "Reimbursement", "Rent Income",
            "Royalties", "Salary", "Scholarship", "Selling Items", "Side Hustle", "Social Media", "Stipend", "Surveys",
            "Tax Refund", "Tips", "Translation", "Trust Fund", "Tutoring", "Vouchers", "Winnings", "YouTube"
        )

        fun setSpinnerOptions(list: List<String>) {
            val sortedList = list.sorted()
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortedList)
            categorySpinner.adapter = adapter
        }

        // Set default to Expense categories initially
        setSpinnerOptions(expenseCategories)

        // Radio Button logic to switch category types
        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.type_expense -> setSpinnerOptions(expenseCategories)
                R.id.type_income -> setSpinnerOptions(incomeCategories)
                R.id.type_other -> setSpinnerOptions(listOf("Other"))
            }
        }
    }
}
