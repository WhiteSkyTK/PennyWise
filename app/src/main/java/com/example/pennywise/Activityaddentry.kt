package com.example.pennywise

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.pennywise.budget.CategoryLimitAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Activityaddentry : AppCompatActivity() {

    //decleartion
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
    private lateinit var addCategoryLauncher: ActivityResultLauncher<Intent>
    private lateinit var amountError: TextView
    private lateinit var categoryError: TextView
    private lateinit var categoryLimitAdapter: CategoryLimitAdapter

    private var pendingCategorySelection: String? = null
    private var selectedPhotoUri: Uri? = null
    private var currentPhotoPath: String = ""
    private val calendar = Calendar.getInstance()
    private var editingTransactionId: Long = -1L
    private var categoriesList = listOf<Category>()

    //get email
    private val loggedInUserId: String by lazy {
        val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        sharedPref.getString("loggedInUserId", "unknownUID") ?: "unknownUID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true // <-- This is the key part!
        }
        db.firestoreSettings = settings
        editingTransactionId = intent.getLongExtra("transactionId", -1L)

        //layout settings
        setContentView(R.layout.activity_add_entry)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //hide bar
        supportActionBar?.hide()


        //calls
        initViews()
        findViewById<RadioButton>(R.id.type_expense).isChecked = true
        setupInitialDate()
        setupCategorySpinner()
        setupListeners()

        val intent = intent
        val isEdit = intent.getBooleanExtra("isEdit", false)
        Log.d("EditMode", "isEdit: $isEdit")

        if (isEdit) {
            // Set text inputs
            val amount = intent.getDoubleExtra("amount", 0.0)
            amountInput.setText(amount.toString())

            descriptionInput.setText(intent.getStringExtra("description"))

            // Set date
            val dateLong = intent.getLongExtra("date", 0L)
            if (dateLong != 0L) {
                calendar.time = Date(dateLong)
                updateDateButton()
            }

            // Set type (radio button)
            val type = intent.getStringExtra("type")
            val selectedTypeId = when (type) {
                "income" -> R.id.type_income
                "expense" -> R.id.type_expense
                "other" -> R.id.type_other
                else -> R.id.type_expense
            }
            findViewById<RadioButton>(selectedTypeId).isChecked = true
            loadCategoriesByType(type ?: "expense") // Trigger spinner after type is set


            // Set category (after spinner loads)
            pendingCategorySelection = intent.getStringExtra("category")

            // Load photo if available
            val photoUriStr = intent.getStringExtra("photoUri")
            if (!photoUriStr.isNullOrEmpty()) {
                selectedPhotoUri = Uri.parse(photoUriStr)
                photoPreview.setImageURI(selectedPhotoUri)
                photoPreview.visibility = View.VISIBLE
                photoLabel.text = getFileNameFromUri(selectedPhotoUri!!)
                attachPhotoButton.setImageResource(R.drawable.ic_placeholder)
            }

            Log.d("EditCheck", "Loaded edit mode with: amount=$amount, date=$dateLong, type=${intent.getStringExtra("type")}, category=$pendingCategorySelection")
            Log.d("EditEntry", "Loaded: amount=${intent.getStringExtra("amount")}, category=${intent.getStringExtra("category")}")

            // Change button text
            saveEntryBtn.text = "Update Entry"
        }


        addCategoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newCategory = result.data?.getStringExtra("newCategory")
                if (newCategory != null) {
                    // Ensure category is added and update the spinner
                    skipCategoryReload = true
                    pendingCategorySelection = newCategory
                    val selectedType = when (typeRadioGroup.checkedRadioButtonId) {
                        R.id.type_expense -> "expense"
                        R.id.type_income -> "income"
                        R.id.type_other -> "other"
                        else -> "expense"
                    }
                    loadCategoriesByType(selectedType)
                }
            }
        }

        addCategoryText.setOnClickListener {
            val intent = Intent(this, Activityaddcategory::class.java)
            intent.putExtra("fromAddEntry", true) // Mark the origin
            addCategoryLauncher.launch(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    //update date
    private fun setupInitialDate() {
        updateDateButton()
    }

    //update date
    private fun updateDateButton() {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dateButton.text = format.format(calendar.time)
    }

    //date picker
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

    //functions
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
        amountError = findViewById(R.id.amountError)
        categoryError = findViewById(R.id.categoryError)

        findViewById<RadioButton>(R.id.type_expense).isChecked = true

        amountInput.doAfterTextChanged {
            if (!it.isNullOrEmpty()) amountError.visibility = View.GONE
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position != 0) categoryError.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Add currency symbol "R" in front while typing
        amountInput.doAfterTextChanged {
            if (!it.isNullOrEmpty() && !it.toString().startsWith("R")) {
                amountInput.setText("R${it.toString().replace("R", "")}")
                amountInput.setSelection(amountInput.text.length)
            }
        }
    }

    //set category
    private fun setupCategorySpinner() {
        // Make sure 'Expense' is selected by default visually
        findViewById<RadioButton>(R.id.type_expense).isChecked = true

        loadCategoriesByType("expense") // Default

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedType = when (checkedId) {
                R.id.type_expense -> "expense"
                R.id.type_income -> "income"
                R.id.type_other -> "other"
                else -> "expense"
            }
            loadCategoriesByType(selectedType)
        }
    }

    //load categories
    private fun loadCategoriesByType(type: String) {
        val db = FirebaseFirestore.getInstance()
        val userCategoriesRef = db.collection("users").document(loggedInUserId).collection("categories")

        userCategoriesRef
            .whereEqualTo("type", type)
            .get()
            .addOnSuccessListener { documents ->
                // Update categoriesList here with full Category objects
                categoriesList = documents.map {
                    Category(
                        id = it.id,  // Firestore document ID
                        name = it.getString("name") ?: "",
                        type = it.getString("type") ?: ""
                    )
                }.sortedBy { it.name }
                val categoryNames = documents.map { it.getString("name") ?: "" }.sorted()

                val finalList = mutableListOf<String>()
                finalList.add("Please select a category")
                finalList.addAll(categoryNames)

                val adapter = object : ArrayAdapter<String>(
                    this@Activityaddentry,
                    android.R.layout.simple_spinner_item,
                    finalList
                ) {
                    override fun isEnabled(position: Int): Boolean = position != 0

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent) as TextView
                        val textColor = if (position == 0) {
                            Color.GRAY
                        } else {
                            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                                Color.WHITE
                            } else {
                                Color.BLACK
                            }
                        }
                        view.setTextColor(textColor)
                        return view
                    }
                }

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter

                // Select pending category if needed
                pendingCategorySelection?.let {
                    val index = finalList.indexOf(it)
                    categorySpinner.setSelection(if (index != -1) index else 0)
                    pendingCategorySelection = null
                } ?: run {
                    categorySpinner.setSelection(0)
                }

                Log.d("Categories", "Loaded ${documents.size()} categories of type $type from Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("Categories", "Error loading categories from Firebase", e)
                Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
    }

    //rerun function
    private var skipCategoryReload = false

    override fun onResume() {
        super.onResume()
        if (!skipCategoryReload) {
            val selectedType = when (typeRadioGroup.checkedRadioButtonId) {
                R.id.type_expense -> "expense"
                R.id.type_income -> "income"
                R.id.type_other -> "other"
                else -> "expense"
            }
            loadCategoriesByType(selectedType)
        } else {
            skipCategoryReload = false
        }
    }

    //button function
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        dateButton.setOnClickListener {
            openDatePicker()
        }

        photoContainer.setOnClickListener {
            checkPermissions()
        }

        addCategoryText.setOnClickListener {
            val intent = Intent(this, Activityaddcategory::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        saveEntryBtn.setOnClickListener {
            saveTransaction()
        }
    }

    //check permissions logic
    private fun checkPermissions() {
        val requiredPermissions = mutableListOf<String>()

        // Check storage permission for images (for Android 13 and below)
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        // Add to the requiredPermissions list if not granted
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(storagePermission)
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CAMERA)
        }

        // Request permissions if any are missing
        if (requiredPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            showImagePickerOptions() // Permissions granted, proceed with the image picker
        }
    }

    //Choose gallery and take photo
    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Attach Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val photoFile: File = createImageFile()
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "${applicationContext.packageName}.fileprovider", // Ensure your manifest + xml is set up
                            photoFile
                        )
                        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        }
                        cameraLauncher.launch(takePictureIntent)
                    }

                    1 -> {
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = "image/*"
                        }
                        galleryLauncher.launch(intent)
                    }
                }
            }.show()
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
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", ".jpg", storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    //save transactions
    private fun saveTransaction() {
        val isEdit = intent.getBooleanExtra("isEdit", false)
        val amountText = amountInput.text.toString().replace("R", "")
        val amount = amountText.toDoubleOrNull()
        val selectedCategoryName = categorySpinner.selectedItem?.toString() ?: ""

        var valid = true
        amountError.visibility = View.GONE
        categoryError.visibility = View.GONE

        if (amount == null || amount <= 0.0) {
            amountError.text = if (amount == null) "Please enter a valid amount" else "Amount must be greater than 0"
            amountError.visibility = View.VISIBLE
            valid = false
        }

        if (selectedCategoryName == "Please select a category" || selectedCategoryName.isEmpty()) {
            categoryError.text = "Please select a valid category"
            categoryError.visibility = View.VISIBLE
            valid = false
        }

        if (!valid) return

        val selectedCategory = categoriesList.find { it.name == selectedCategoryName }
        val selectedCategoryId = selectedCategory?.id ?: ""

        val type = when (typeRadioGroup.checkedRadioButtonId) {
            R.id.type_expense -> "expense"
            R.id.type_income -> "income"
            else -> "other"
        }

        val description = descriptionInput.text.toString()
        val dateInMillis = calendar.timeInMillis
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Calendar.getInstance().time)

        val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        val userId = sharedPref.getString("loggedInUserId", null)

        if (userId == null) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a reference for user's transaction subcollection
        val userTransactionsRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("transactions")

        // Handle photo saving (optional upgrade: Firebase Storage later)
        val savedImagePath = selectedPhotoUri?.let { saveImageToInternalStorage(it) } ?: ""

        val calendarForMonthYear = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
        }
        val month = calendarForMonthYear.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        val year = calendarForMonthYear.get(Calendar.YEAR)
        val monthYear = String.format("%04d-%02d", year, month) // e.g. "2025-05"

        val transactionData = hashMapOf(
            "userId" to userId,
            "amount" to amount,
            "type" to type,
            "category" to selectedCategoryName,
            "categoryId" to selectedCategoryId,
            "description" to description,
            "date" to dateInMillis,
            "startTime" to currentTime,
            "endTime" to currentTime,
            "photoPath" to savedImagePath,
            "monthYear" to monthYear
        )

        if (isEdit) {
            val transactionId = intent.getStringExtra("transactionId")
            if (!transactionId.isNullOrEmpty()) {
                userTransactionsRef.document(transactionId)
                    .set(transactionData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()

                        val transaction = Transaction(
                            id = transactionId,
                            userId = userId,
                            amount = amount!!,
                            type = type,
                            category = selectedCategoryName,
                            categoryId = selectedCategoryId,
                            description = description,
                            date = dateInMillis,
                            startTime = currentTime,
                            endTime = currentTime,
                            photoPath = savedImagePath,
                            monthYear = monthYear
                        )
                        updateUsedAmountAfterTransaction(this, transaction) {
                            fetchCategoryLimits()
                            AddCategory.shouldRefreshOnResume = true
                            startMainActivity()
                        }

                        AddCategory.shouldRefreshOnResume = true
                        startMainActivity()
                    }

            } else {
                Toast.makeText(this, "Edit mode: document ID missing", Toast.LENGTH_SHORT).show()
            }
        } else {
            userTransactionsRef.add(transactionData)
                .addOnSuccessListener { documentRef ->
                    Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()

                    // Update the related category limit's used amount
                    val transaction = Transaction(
                        id = documentRef.id,
                        userId = userId,
                        amount = amount!!,
                        type = type,
                        category = selectedCategoryName,
                        categoryId = selectedCategoryId,
                        description = description,
                        date = dateInMillis,
                        startTime = currentTime,
                        endTime = currentTime,
                        photoPath = savedImagePath,
                        monthYear = monthYear
                    )
                    updateUsedAmountAfterTransaction(this, transaction) {
                        fetchCategoryLimits()
                        AddCategory.shouldRefreshOnResume = true
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }

                    AddCategory.shouldRefreshOnResume = true
                    intent.putExtra("reload_budget", true)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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

    // Permission request launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            showImagePickerOptions() // Proceed with the image picker if permissions are granted
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    //cameralancher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            photoPreview.setImageBitmap(bitmap)
            photoPreview.visibility = View.VISIBLE
            photoLabel.text = File(currentPhotoPath).name
            attachPhotoButton.setImageResource(R.drawable.ic_placeholder)
            selectedPhotoUri = Uri.fromFile(File(currentPhotoPath))
        }
    }

    //gallerylauncher
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

    fun updateUsedAmountAfterTransaction(
        context: Context,
        transaction: Transaction,
        onComplete: () -> Unit // callback
    ) {
        val userId = transaction.userId
        val categoryId = transaction.categoryId
        val amount = transaction.amount ?: return
        if (transaction.type != "expense") return

        val month = transaction.monthYear ?: return
        val docId = "${categoryId}_$month"

        val db = FirebaseFirestore.getInstance()
        val categoryLimitRef = db.collection("users")
            .document(userId)
            .collection("categoryLimits")
            .document(docId)

        db.runTransaction { transactionFirestore ->
            val snapshot = transactionFirestore.get(categoryLimitRef)
            if (!snapshot.exists()) {
                val newLimit = hashMapOf(
                    "userId" to userId,
                    "categoryId" to categoryId,
                    "category" to transaction.category,
                    "month" to month,
                    "minAmount" to 0.0,
                    "maxAmount" to 0.0,
                    "usedAmount" to amount
                )
                transactionFirestore.set(categoryLimitRef, newLimit)
            } else {
                val currentUsed = snapshot.getDouble("usedAmount") ?: 0.0
                val newUsed = currentUsed + amount
                transactionFirestore.update(categoryLimitRef, "usedAmount", newUsed)
            }
        }.addOnSuccessListener {
            Toast.makeText(context, "Category limit updated", Toast.LENGTH_SHORT).show()
            onComplete() //  call the callback
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to update limit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCategoryLimits() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("categoryLimits")
            .get()
            .addOnSuccessListener { snapshot ->
                val limits = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CategoryLimit::class.java)
                }
                //categoryLimitAdapter.updateData(limits)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch limits", Toast.LENGTH_SHORT).show()
            }
    }

}