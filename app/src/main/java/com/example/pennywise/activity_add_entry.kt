package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class activity_add_entry : AppCompatActivity() {

    private lateinit var dateButton: Button
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var categorySpinner: Spinner
    private lateinit var addCategoryText: TextView
    private lateinit var amountInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var attachPhotoButton: ImageButton
    private lateinit var photoPreview: ImageView
    private lateinit var saveEntryBtn: Button
    private lateinit var backButton: ImageButton
    private lateinit var saveButton: ImageButton

    // Assuming the user email is passed from the login session
    private val userEmail: String = "user@example.com"  // This should come from your authentication system

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_entry)

        // Initialize views
        dateButton = findViewById(R.id.dateButton)
        typeRadioGroup = findViewById(R.id.typeRadioGroup)
        categorySpinner = findViewById(R.id.categorySpinner)
        addCategoryText = findViewById(R.id.addCategoryText)
        amountInput = findViewById(R.id.amountInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        attachPhotoButton = findViewById(R.id.attachPhotoButton)
        photoPreview = findViewById(R.id.photoPreview)
        saveEntryBtn = findViewById(R.id.saveEntryBtn)
        backButton = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)

        // Handle Category Adding
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
            "Tuition", "Water", "WiFi", "Other")

        val incomeCategories = listOf("Affiliate", "Allowance", "Blog", "Bonus", "Cashback", "Coding", "Commission", "Consulting", "Digital Art",
            "Dividends", "Dog Walking", "Donations", "Dropshipping", "eBook", "Event Hosting", "Freelance", "Gift",
            "Grant", "Hair Braiding", "Income", "Interest", "Investment", "Lottery", "Online Courses", "Other",
            "Part-time Job", "Passive Income", "Pension", "Prize", "Profit", "Refund", "Reimbursement", "Rent Income",
            "Royalties", "Salary", "Scholarship", "Selling Items", "Side Hustle", "Social Media", "Stipend", "Surveys",
            "Tax Refund", "Tips", "Translation", "Trust Fund", "Tutoring", "Vouchers", "Winnings", "YouTube")

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

        // Save Entry logic
        saveEntryBtn.setOnClickListener {
            val amount = amountInput.text.toString().toDoubleOrNull()
            val type = when (typeRadioGroup.checkedRadioButtonId) {
                R.id.type_expense -> "expense"
                R.id.type_income -> "income"
                else -> "other"
            }
            val category = categorySpinner.selectedItem.toString()
            val description = descriptionInput.text.toString()
            val date = System.currentTimeMillis() // Current timestamp
            val startTime = "12:00 PM" // Placeholder, add actual logic to get start time
            val endTime = "12:30 PM" // Placeholder, add actual logic to get end time
            val photoUri = "" // Placeholder for the photo URI if the user attaches an image

            if (amount != null) {
                val transaction = Transaction(
                    userEmail = userEmail,
                    amount = amount,
                    type = type,
                    category = category,
                    description = description,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    photoUri = photoUri
                )

                // Insert transaction into the database
                lifecycleScope.launch {
                    val transactionDao = AppDatabase.getDatabase(this@activity_add_entry).transactionDao()
                    transactionDao.insertTransaction(transaction)
                    Toast.makeText(this@activity_add_entry, "Transaction saved", Toast.LENGTH_SHORT).show()

                    // Optionally, go back to previous screen or clear fields
                    finish() // This will close the activity after saving
                }
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
