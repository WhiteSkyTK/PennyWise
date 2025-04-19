package com.example.pennywise

import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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

    private var selectedPhotoUri: Uri? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    private val IMAGE_PICK_CODE = 1001

    // Simulating email for now (should be passed from intent/session)
    private val userEmail: String by lazy {
        intent.getStringExtra("USER_EMAIL") ?: "unknown@example.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_entry)

        initViews()
        setDefaultDate()
        setupCategorySpinner()
        setupListeners()
    }

    private fun setupInitialDate() {
        updateDateButton()
    }

    private fun updateDateButton() {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dateButton.text = format.format(calendar.time)
    }

    private fun openDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, y, m, d ->
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)
            updateDateButton()
        }, year, month, day)

        datePicker.show()
    }

    private fun initViews() {
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

        // Add currency symbol "R" in front while typing
        amountInput.doAfterTextChanged {
            if (!it.isNullOrEmpty() && !it.toString().startsWith("R")) {
                amountInput.setText("R${it.toString().replace("R", "")}")
                amountInput.setSelection(amountInput.text.length)
            }
        }
    }

    private fun setDefaultDate() {
        val currentDate = dateFormat.format(Date())
        dateButton.text = currentDate
    }

    private fun setupCategorySpinner() {
        setSpinnerOptions(expenseCategories)

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.type_expense -> setSpinnerOptions(expenseCategories)
                R.id.type_income -> setSpinnerOptions(incomeCategories)
                R.id.type_other -> setSpinnerOptions(listOf("Other"))
            }
        }
    }

    private fun setSpinnerOptions(list: List<String>) {
        val sortedList = list.sorted()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortedList)
        categorySpinner.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        dateButton.setOnClickListener {
            openDatePicker()
        }

        attachPhotoButton.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickIntent, IMAGE_PICK_CODE)
        }

        addCategoryText.setOnClickListener {
            startActivity(Intent(this, activity_add_category::class.java))
        }

        saveEntryBtn.setOnClickListener {
            saveTransaction()
        }

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.type_expense -> setSpinnerOptions(expenseCategories)
                R.id.type_income -> setSpinnerOptions(incomeCategories)
                R.id.type_other -> setSpinnerOptions(listOf("Other"))
            }
        }
    }

    private fun saveTransaction() {
        val amountText = amountInput.text.toString().replace("R", "")
        val amount = amountText.toDoubleOrNull()

        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val type = when (typeRadioGroup.checkedRadioButtonId) {
            R.id.type_expense -> "expense"
            R.id.type_income -> "income"
            else -> "other"
        }

        val category = categorySpinner.selectedItem.toString()
        val description = descriptionInput.text.toString()
        val dateInMillis = calendar.timeInMillis
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = timeFormat.format(Calendar.getInstance().time)

        val savedImagePath = selectedPhotoUri?.let { saveImageToInternalStorage(it) } ?: ""

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "unknown@example.com"

        val transaction = Transaction(
            userEmail = userEmail,
            amount = amount,
            type = type,
            category = category,
            description = description,
            date = dateInMillis,
            startTime = time,
            endTime = time,
            photoUri = savedImagePath
        )

        lifecycleScope.launch {
            AppDatabase.getDatabase(this@activity_add_entry).transactionDao()
                .insertTransaction(transaction)

            Toast.makeText(this@activity_add_entry, "Transaction saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Save picked image to internal storage
    private fun saveImageToInternalStorage(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            selectedPhotoUri = data?.data
            photoPreview.setImageURI(selectedPhotoUri)
        }
    }

    private val expenseCategories = listOf(
        "Accessories", "Alcohol", "Baby Supplies", "Bills", "Books", "Car", "Charity", "Clothes",
        "Coffee", "Delivery", "Dining Out", "Electricity", "Fine", "Fuel", "Gaming", "Gifts",
        "Groceries", "Gym", "Haircut", "Hardware", "Healthcare", "Insurance", "Internet", "Laundry",
        "Lottery", "Miscellaneous", "Movies", "Music", "Parking", "Pet Care", "Phone", "Public Transport",
        "Rent", "Repairs", "Snacks", "Software", "Spa", "Stationery", "Streaming", "Subscription",
        "Taxi", "Tools", "Toys", "Travel", "Tuition", "Water", "WiFi", "Other"
    )

    private val incomeCategories = listOf(
        "Affiliate", "Allowance", "Blog", "Bonus", "Cashback", "Coding", "Commission", "Consulting",
        "Digital Art", "Dividends", "Dog Walking", "Donations", "Dropshipping", "eBook", "Event Hosting",
        "Freelance", "Gift", "Grant", "Hair Braiding", "Income", "Interest", "Investment", "Lottery",
        "Online Courses", "Other", "Part-time Job", "Passive Income", "Pension", "Prize", "Profit",
        "Refund", "Reimbursement", "Rent Income", "Royalties", "Salary", "Scholarship", "Selling Items",
        "Side Hustle", "Social Media", "Stipend", "Surveys", "Tax Refund", "Tips", "Translation",
        "Trust Fund", "Tutoring", "Vouchers", "Winnings", "YouTube"
    )
}
