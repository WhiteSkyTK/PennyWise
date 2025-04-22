package com.example.pennywise

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    private lateinit var photoContainer: LinearLayout
    private lateinit var photoLabel: TextView

    private var selectedPhotoUri: Uri? = null
    private var currentPhotoPath: String = ""
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    // Permission request launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImagePickerOptions()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            photoPreview.setImageBitmap(bitmap)
            photoPreview.visibility = View.VISIBLE
            photoLabel.text = File(currentPhotoPath).name
            attachPhotoButton.setImageResource(R.drawable.ic_placeholder) // Optional: change icon
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri
                photoPreview.setImageURI(uri)
                photoPreview.visibility = View.VISIBLE
                photoLabel.text = getFileNameFromUri(uri)
                attachPhotoButton.setImageResource(R.drawable.ic_placeholder) // Optional: change icon
            }
        }
    }

    // Simulating email for now (should be passed from intent/session)
    private val userEmail: String by lazy {
        intent.getStringExtra("USER_EMAIL") ?: "unknown@example.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_add_entry)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
        photoContainer = findViewById(R.id.photoContainer)
        photoLabel = findViewById(R.id.photoLabel)

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

        photoContainer.setOnClickListener {
            checkPermissions()
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

    private fun checkPermissions() {
        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
            showImagePickerOptions()
        } else {
            requestPermissionLauncher.launch(requiredPermission)
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> checkCameraPermission()
                1 -> openGallery()
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        val photoURI = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun saveTransaction() {
        val amountText = amountInput.text.toString().replace("R", "")
        val amount = amountText.toDoubleOrNull()
        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("loggedInUserEmail", "unknown@example.com") ?: "unknown@example.com"


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

            Log.d("AddEntry", "Saved: $transaction")
            Log.d("EmailCheck", "userEmail = $userEmail")

            Toast.makeText(this@activity_add_entry, "Transaction saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result ?: "image_${System.currentTimeMillis()}"
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
