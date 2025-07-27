package com.tk.pennywise

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

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
    private lateinit var saveEntryProgressBar: ProgressBar // Add this if you used Step 1
    private var originalSaveButtonText: String = "" // To store the button's original text

    private var pendingCategorySelection: String? = null
    private var selectedPhotoUri: Uri? = null
    private var currentPhotoPath: String = ""
    private val calendar = Calendar.getInstance()
    //private var editingTransactionId: Long = -1L
    private var editingTransactionDocId: String? = null
    private var categoriesList = listOf<Category>()
    private var lastSelectedCategoryName: String? = null

    private val db = FirebaseFirestore.getInstance()

    //get email
    private val loggedInUserId: String by lazy {
        // Use FirebaseAuth directly for consistency and real-time user state
        FirebaseAuth.getInstance().currentUser?.uid ?: run {
            // Fallback to SharedPreferences if needed, but prefer FirebaseAuth
            val sharedPref = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
            sharedPref.getString("loggedInUserId", "unknownUID") ?: "unknownUID"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        editingTransactionDocId = intent.getStringExtra("transactionId")

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

        val isEdit = editingTransactionDocId != null // Determine if it's edit mode
        Log.d("AddEntryMode", "isEdit: $isEdit, transactionId: $editingTransactionDocId")

        if (isEdit) {
            // Set text inputs
            val amount = intent.getDoubleExtra("amount", 0.0)
            amountInput.setText(if (amount > 0) String.format(Locale.US, "%.2f", amount) else "") // Format amount

            descriptionInput.setText(intent.getStringExtra("description"))

            // Set date
            val dateLong = intent.getLongExtra("date", 0L)
            if (dateLong != 0L) {
                calendar.time = Date(dateLong)
                updateDateButton()
            }

            // Set type (radio button) AND THEN load categories
            val typeFromIntent = intent.getStringExtra("type")
            Log.d("AddEntryEdit", "Type from Intent: $typeFromIntent")

            val selectedTypeId: Int
            val typeForCategoryLoad: String

            when (typeFromIntent) {
                "income" -> {
                    selectedTypeId = R.id.type_income
                    typeForCategoryLoad = "income"
                }
                "other" -> {
                    selectedTypeId = R.id.type_other
                    typeForCategoryLoad = "other"
                }
                "expense" -> {
                    selectedTypeId = R.id.type_expense
                    typeForCategoryLoad = "expense"
                }
                else -> { // Default to expense if type is null or unexpected
                    selectedTypeId = R.id.type_expense
                    typeForCategoryLoad = "expense"
                    Log.w("AddEntryEdit", "Type from intent was null or unexpected ('$typeFromIntent'), defaulting to 'expense'")
                }
            }

            // Set pending category BEFORE programmatically checking the radio button.
            // This ensures it's available when the listener (potentially) fires.
            pendingCategorySelection = intent.getStringExtra("category")
            Log.d("AddEntryEdit", "Pending Category from Intent set to: $pendingCategorySelection")

            Log.d("AddEntryEdit", "Before setting radio button for edit. Type from Intent: $typeFromIntent, Target RadioButton ID: $selectedTypeId")
            findViewById<RadioButton>(selectedTypeId).isChecked = true // This should trigger the OnCheckedChangeListener
            Log.d("AddEntryEdit", "After setting radio button for edit. Checked RadioButton ID: ${typeRadioGroup.checkedRadioButtonId}")

            // Load photo if available
            val photoUriStr = intent.getStringExtra("photoUri")
            if (!photoUriStr.isNullOrEmpty()) {
                selectedPhotoUri = Uri.parse(photoUriStr)
                try {
                    photoPreview.setImageURI(selectedPhotoUri)
                    photoPreview.visibility = View.VISIBLE
                    photoLabel.text = getFileNameFromUri(selectedPhotoUri!!)
                    attachPhotoButton.setImageResource(R.drawable.ic_placeholder)
                } catch (e: Exception) {
                    Log.e("AddEntry", "Error loading image URI for edit: $selectedPhotoUri", e)
                    photoPreview.visibility = View.GONE
                    photoLabel.text = "Attach Photo"
                    attachPhotoButton.setImageResource(R.drawable.ic_attach_photo)
                }
            }
            saveEntryBtn.text = "Update Entry"
            originalSaveButtonText = "Update Entry"
        } else {
            originalSaveButtonText = "Save Entry"
            // For new entries, ensure the default "expense" categories are loaded.
            // This is handled by setupCategorySpinner and the default checked radio button.
        }

        addCategoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newCategoryName = result.data?.getStringExtra("newCategory")
                if (newCategoryName != null) {
                    skipCategoryReload = true // Prevent immediate reload in onResume
                    pendingCategorySelection = newCategoryName
                    val selectedType = when (typeRadioGroup.checkedRadioButtonId) {
                        R.id.type_expense -> "expense"
                        R.id.type_income -> "income"
                        R.id.type_other -> "other"
                        else -> "expense" // Default
                    }
                    // This will add the new category to categoriesList and update the spinner
                    loadCategoriesByType(selectedType, true)
                }
            }
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
        saveEntryBtn = findViewById(R.id.saveEntryBtn)
        saveEntryProgressBar = findViewById(R.id.saveEntryProgressBar) // Add this if you used Step 1
        originalSaveButtonText = saveEntryBtn.text.toString() // Store original text


        findViewById<RadioButton>(R.id.type_expense).isChecked = true

        amountInput.doAfterTextChanged {
            if (!it.isNullOrEmpty()) amountError.visibility = View.GONE
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position != 0) categoryError.visibility = View.GONE
                lastSelectedCategoryName = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Add currency symbol "R" in front while typing
        amountInput.doAfterTextChanged {
            if (!it.isNullOrEmpty() && !it.toString().startsWith("R")) {
                amountInput.setText("R${it.toString().replace("R", "")}")
                amountInput.setSelection(amountInput.text.length)
            } else if (it.toString() == "R") {
                // Prevent just "R" if user deletes numbers
                // amountInput.setText("")
            }
        }
    }

    private fun setSaveButtonLoadingState(isLoading: Boolean) {
        if (isLoading) {
            saveEntryBtn.isEnabled = false
            // Option A: Just change text (if not using ProgressBar in XML)
            // saveEntryBtn.text = "Saving..."

            // Option B: Use ProgressBar (if you added it in XML)
            originalSaveButtonText = saveEntryBtn.text.toString() // Save current text before blanking
            saveEntryBtn.text = "" // Clear text to show ProgressBar
            saveEntryProgressBar.visibility = View.VISIBLE

        } else {
            saveEntryBtn.isEnabled = true
            // Option A: Reset text
            // saveEntryBtn.text = originalSaveButtonText // Reset to "Save Entry" or "Update Entry"

            // Option B: Hide ProgressBar and reset text
            saveEntryProgressBar.visibility = View.GONE
            saveEntryBtn.text = originalSaveButtonText
        }
    }

    //set category
    private fun setupCategorySpinner() {
        // Determine initial type (expense by default, or from edit mode if already processed)
        val initialType = if (editingTransactionDocId != null) {
            // This part might run BEFORE the radio button is set in the isEdit block
            // So, let's rely on the currently checked radio button if possible,
            // or the intent if the radio group hasn't been fully initialized.
            when (typeRadioGroup.checkedRadioButtonId) { // Check current state first
                R.id.type_income -> "income"
                R.id.type_other -> "other"
                R.id.type_expense -> "expense"
                else -> { // Fallback to intent if radio group not reflecting edit mode yet
                    when (intent.getStringExtra("type")) {
                        "income" -> "income"
                        "other" -> "other"
                        else -> "expense" // Default for new or if intent type is missing
                    }
                }
            }
        } else {
            "expense" // Default for new entries
        }
        Log.d("SetupSpinner", "Initial type for category load: $initialType. Pending: $pendingCategorySelection")
        loadCategoriesByType(initialType) // Load categories for the initial/current type

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedType = when (checkedId) {
                R.id.type_expense -> "expense"
                R.id.type_income -> "income"
                R.id.type_other -> "other"
                else -> "expense" // Default
            }
            Log.d("TypeChange", "Type changed to: $selectedType. Reloading categories. Pending: $pendingCategorySelection")
            loadCategoriesByType(selectedType) // pendingCategorySelection will be used if set
        }
    }

    //load categories
    private fun loadCategoriesByType(type: String, forceSelectPending: Boolean = false) {
        if (loggedInUserId == "unknownUID") {
            Log.e("Categories", "User ID is unknown, cannot load categories.")
            Toast.makeText(this, "Cannot load categories: User not identified.", Toast.LENGTH_SHORT).show()
            // Populate spinner with just the placeholder
            val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Please select a category"))
            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = emptyAdapter
            categorySpinner.setSelection(0)
            return
        }

        val userCategoriesRef = db.collection("users").document(loggedInUserId).collection("categories")

        userCategoriesRef
            .whereEqualTo("type", type)
            .get()
            .addOnSuccessListener { documents ->
                categoriesList = documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id) // Ensure Category has id
                }.sortedBy { it.name }

                val categoryNames = categoriesList.map { it.name }
                val finalList = mutableListOf("Please select a category")
                finalList.addAll(categoryNames)

                val adapter = object : ArrayAdapter<String>(
                    this@Activityaddentry,
                    android.R.layout.simple_spinner_item,
                    finalList
                ) {
                    override fun isEnabled(position: Int): Boolean = position != 0
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent) as TextView
                        view.setTextColor(
                            if (position == 0) Color.GRAY
                            else if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE
                            else Color.BLACK
                        )
                        return view
                    }
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter

                var selectionMade = false
                if (pendingCategorySelection != null) {
                    val index = finalList.indexOf(pendingCategorySelection)
                    if (index != -1) {
                        // Check if the pending category actually belongs to the *current* type being loaded
                        val categoryObjectForPending = categoriesList.find { it.name == pendingCategorySelection }
                        if (categoryObjectForPending != null /* && categoryObjectForPending.type == type */) { // Type check already implicitly handled by the query
                            categorySpinner.setSelection(index)
                            lastSelectedCategoryName = pendingCategorySelection
                            selectionMade = true
                            Log.d("Categories", "Successfully set spinner to PENDING category '$pendingCategorySelection' for type '$type' at index $index.")
                        } else {
                            // This means pendingCategorySelection name exists, but not for this type.
                            // This scenario shouldn't happen if categories are correctly filtered by type.
                            Log.w("Categories", "Pending category '$pendingCategorySelection' found by name but not in the loaded list for type '$type'. Resetting.")
                            categorySpinner.setSelection(0)
                        }
                    } else {
                        // If pendingCategorySelection was for a different type and now doesn't exist for the current type
                        categorySpinner.setSelection(0)
                        Log.w("Categories", "Pending category '$pendingCategorySelection' NOT FOUND in list for type '$type'. Resetting.")
                    }
                    // Clear pendingCategorySelection AFTER attempting to use it for THIS load.
                    // Only clear if not forced by addCategoryLauncher callback, which has its own logic.
                    if (!forceSelectPending) {
                        pendingCategorySelection = null
                    }
                } else {
                    categorySpinner.setSelection(0) // Default to placeholder if no pending selection
                }

                // If pendingSelection didn't apply (or was null), try lastSelectedCategoryName
                // This part is more for onResume scenarios or if type changes interactively
                if (!selectionMade && lastSelectedCategoryName != null && lastSelectedCategoryName != "Please select a category") {
                    val index = finalList.indexOf(lastSelectedCategoryName)
                    // Ensure lastSelectedCategoryName is valid for the current type
                    if (index != -1 && categoriesList.any { it.name == lastSelectedCategoryName /* && it.type == type */}) {
                        categorySpinner.setSelection(index)
                        selectionMade = true
                        Log.d("Categories", "Successfully set spinner to LAST SELECTED category '$lastSelectedCategoryName' for type '$type' at index $index.")
                    } else {
                        // lastSelectedCategoryName was for a different type or no longer exists
                        // Allow it to fall through to default selection
                        // lastSelectedCategoryName = null // Clear it if not found in current list for this type
                        Log.d("Categories", "Last selected category '$lastSelectedCategoryName' not found or not applicable for type '$type'.")
                    }
                }

                if (!selectionMade) {
                    categorySpinner.setSelection(0) // Default to placeholder if no selection could be made
                    Log.d("Categories", "No specific category selected, defaulting to placeholder for type '$type'.")
                }
                Log.d("Categories", "Loaded ${categoriesList.size} categories of type $type. Spinner updated.")
            }
            .addOnFailureListener { e ->
                Log.e("Categories", "Error loading categories from Firebase for type $type", e)
                Toast.makeText(this, "Failed to load categories for $type", Toast.LENGTH_SHORT).show()
            }
    }

    //rerun function
    private var skipCategoryReload = false
    override fun onResume() {
        super.onResume()
        Log.d("AddEntryResume", "onResume called. skipCategoryReload: $skipCategoryReload, pendingCategory: $pendingCategorySelection, lastSelected: $lastSelectedCategoryName")
        val selectedType = when (typeRadioGroup.checkedRadioButtonId) {
            R.id.type_expense -> "expense"
            R.id.type_income -> "income"
            R.id.type_other -> "other"
            else -> "expense"
        }
        Log.d("AddEntryResume", "Current UI selected type in onResume: $selectedType") // <--- CORRECTED HERE

        if (skipCategoryReload) {
            // This was true because we just returned from Activityaddcategory
            // loadCategoriesByType would have been called with forceSelectPending = true
            // by the addCategoryLauncher callback.
            // We ensure pendingCategorySelection is used and then reset skipCategoryReload.
            val currentPending = pendingCategorySelection
            loadCategoriesByType(selectedType, true) // Force it to use pendingCategorySelection
            // If loadCategoriesByType with forceSelectPending=true is meant to consume pendingCategorySelection,
            // you might not need to restore it. Test this behavior.
            // If it's cleared prematurely, then restoring might be needed.
            // pendingCategorySelection = currentPending // Consider if this is needed based on loadCategoriesByType
            skipCategoryReload = false
            Log.d("AddEntryResume", "skipCategoryReload was true. Called loadCategoriesByType with forceSelect. Pending after: $pendingCategorySelection")
        } else {
            // Normal onResume, e.g., returning from gallery or just resuming the app
            // lastSelectedCategoryName should handle restoring the selection if pending is null
            Log.d("AddEntryResume", "skipCategoryReload was false. Calling loadCategoriesByType. Pending before: $pendingCategorySelection")
            loadCategoriesByType(selectedType)
        }
    }


    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val rootView = findViewById<View>(android.R.id.content)
        val cx = rootView.width / 2
        val cy = rootView.height / 2

        val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

        val anim = ViewAnimationUtils.createCircularReveal(rootView, cx, cy, initialRadius, 0f)
        anim.duration = 300

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                rootView.visibility = View.INVISIBLE
                finish()
                overridePendingTransition(0, 0)
            }
        })

        anim.start()
    }

    //button function
    private fun setupListeners() {
        backButton.setOnClickListener {
            // Use custom onBackPressed for animation consistency
            onBackPressed() // Calls your custom animated back press
        }
        dateButton.setOnClickListener { openDatePicker() }
        photoContainer.setOnClickListener { checkPermissions() } // For the whole container
        attachPhotoButton.setOnClickListener { checkPermissions() } // Also for the button itself

        addCategoryText.setOnClickListener {
            val intent = Intent(this, Activityaddcategory::class.java)
            intent.putExtra("fromAddEntry", true)
            addCategoryLauncher.launch(intent) // Use launcher
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        saveEntryBtn.setOnClickListener {
            Log.d("SaveTransaction", "Save button clicked.")
            saveTransaction()
        }
    }

    //check permissions logic
    private fun checkPermissions() {
        val requiredPermissions = mutableListOf<String>()
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE // For API < 33, WRITE is implied by READ
        }
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(storagePermission)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CAMERA)
        }

        if (requiredPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            showImagePickerOptions()
        }
    }

    //Choose gallery and take photo
    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        AlertDialog.Builder(this)
            .setTitle("Attach Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Take Photo
                        try {
                            val photoFile: File = createImageFile()
                            val photoURI: Uri = FileProvider.getUriForFile(
                                this,
                                "${applicationContext.packageName}.fileprovider",
                                photoFile
                            )
                            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            }
                            cameraLauncher.launch(takePictureIntent)
                        } catch (ex: IOException) {
                            Log.e("ImagePicker", "Error creating image file", ex)
                            Toast.makeText(this, "Could not create image file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> { // Choose from Gallery
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                            type = "image/*" // Ensure only images are selectable
                        }
                        galleryLauncher.launch(intent)
                    }
                    2 -> { // Remove Photo
                        selectedPhotoUri = null
                        currentPhotoPath = ""
                        photoPreview.setImageDrawable(null) // Clear preview
                        photoPreview.visibility = View.GONE
                        photoLabel.text = "Attach Photo" // Reset label
                        attachPhotoButton.setImageResource(R.drawable.ic_attach_photo) // Reset icon (replace with your default)
                    }
                }
            }.show()
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
        val amountText = amountInput.text.toString().replace("R", "").trim()
        val amount = amountText.toDoubleOrNull()
        val selectedCategoryName = if (categorySpinner.selectedItemPosition > 0) {
            categorySpinner.selectedItem.toString()
        } else {
            ""
        }

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

        if (!valid) {
            Log.d("SaveTransaction", "Validation failed.")
            return
        }

        // --- START LOADING STATE ---
        setSaveButtonLoadingState(true)

        val selectedCategoryObject = categoriesList.find { it.name == selectedCategoryName }
        if (selectedCategoryObject == null) {
            Log.e("SaveTransaction", "Selected category object not found in categoriesList for name: $selectedCategoryName")
            Toast.makeText(this, "Error: Category details not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategoryId = selectedCategoryObject.id // This should be the Firestore doc ID of the category

        val type = when (typeRadioGroup.checkedRadioButtonId) {
            R.id.type_expense -> "expense"
            R.id.type_income -> "income"
            else -> "other"
        }

        val description = descriptionInput.text.toString()
        val dateInMillis = calendar.timeInMillis
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Calendar.getInstance().time)

        if (loggedInUserId == "unknownUID") {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        // Create a reference for user's transaction subcollection
        val userTransactionsRef = db.collection("users").document(loggedInUserId).collection("transactions")
        val savedImageInternalPath = selectedPhotoUri?.let { saveImageToInternalStorage(it) } ?: ""

        val calendarForMonthYear = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
        }
        val month = calendarForMonthYear.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        val year = calendarForMonthYear.get(Calendar.YEAR)
        val monthYear = String.format("%04d-%02d", year, month) // e.g. "2025-05"

        val transactionData = hashMapOf(
            "userId" to loggedInUserId,
            "amount" to amount,
            "type" to type,
            "category" to selectedCategoryName,
            "categoryId" to selectedCategoryId,
            "description" to description,
            "date" to dateInMillis,
            "startTime" to currentTime,
            "endTime" to currentTime,
            "photoPath" to savedImageInternalPath,
            "monthYear" to monthYear
        )

        // Disable button to prevent multiple clicks
        saveEntryBtn.isEnabled = false

        if (isEdit && editingTransactionDocId != null) {
            userTransactionsRef.document(editingTransactionDocId!!)
                .set(transactionData) // Or update() if you only change specific fields
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
                    val updatedTransaction = Transaction(
                        id = editingTransactionDocId!!, // Firestore document ID
                        userId = loggedInUserId,
                        amount = amount!!,
                        type = type,
                        category = selectedCategoryName,
                        categoryId = selectedCategoryId,
                        description = description,
                        date = dateInMillis,
                        startTime = currentTime,
                        endTime = currentTime,
                        photoPath = savedImageInternalPath,
                        monthYear = monthYear
                        // startTime and endTime are likely not needed in Transaction data class
                    )

                    // Lambda to handle finishing and navigating
                    val finishAndGoToMain = {
                        setSaveButtonLoadingState(false)
                        val resultIntent = Intent()
                        resultIntent.putExtra("transaction_added_month", updatedTransaction.monthYear)
                        resultIntent.putExtra("needs_refresh", true)
                        // **** ADD THIS EXTRA ****
                        resultIntent.putExtra("go_to_main_after_edit", true)
                        setResult(RESULT_OK, resultIntent)
                        finishAndAnimate() // Your existing finish method
                    }

                    // Update used amount if it's an expense
                    if (updatedTransaction.type == "expense") {
                        updateUsedAmountAfterTransaction(this, updatedTransaction) {
                            Log.d("SaveTransaction", "Edit: updateUsedAmountAfterTransaction completed.")
                            finishAndGoToMain()
                        }
                    } else {
                        // Not an expense, just finish and signal to go to main
                        finishAndGoToMain()
                    }
                }
                .addOnFailureListener { e ->
                    setSaveButtonLoadingState(false)
                    Log.e("SaveTransaction", "Error updating transaction", e)
                    Toast.makeText(this, "Error updating: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else { // New transaction
            userTransactionsRef.add(transactionData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
                    val newTransaction = Transaction(
                        id = documentReference.id, // Firestore document ID
                        userId = loggedInUserId,
                        amount = amount!!,
                        type = type,
                        category = selectedCategoryName,
                        categoryId = selectedCategoryId,
                        description = description,
                        date = dateInMillis,
                        startTime = currentTime,
                        endTime = currentTime,
                        photoPath = savedImageInternalPath, monthYear = monthYear
                    )
                    // Update used amount if it's an expense
                    if (newTransaction.type == "expense") {
                        updateUsedAmountAfterTransaction(this, newTransaction) {
                            Log.d("SaveTransaction", "New: updateUsedAmountAfterTransaction completed.")
                            setSaveButtonLoadingState(false)
                            // Intent to signal budget reload could be useful
                            val resultIntent = Intent()
                            resultIntent.putExtra("transaction_added_month", newTransaction.monthYear) // "YYYY-MM"
                            resultIntent.putExtra("needs_refresh", true)
                            setResult(RESULT_OK, resultIntent)
                            finishAndAnimate()
                        }
                    } else {
                        setSaveButtonLoadingState(false)
                        setResult(RESULT_OK)
                        finishAndAnimate()
                    }
                }
                .addOnFailureListener { e ->
                    setSaveButtonLoadingState(false)
                    Log.e("SaveTransaction", "Error saving transaction", e)
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun finishAndAnimate() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // Or your custom ones
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
            // currentPhotoPath should be set by createImageFile
            if (currentPhotoPath.isNotEmpty()) {
                val photoFile = File(currentPhotoPath)
                selectedPhotoUri = Uri.fromFile(photoFile) // Get URI from the file path
                try {
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath) // Or use URI
                    photoPreview.setImageBitmap(bitmap)
                    photoPreview.visibility = View.VISIBLE
                    photoLabel.text = photoFile.name
                    attachPhotoButton.setImageResource(R.drawable.ic_placeholder)
                } catch (e: Exception) {
                    Log.e("CameraLauncher", "Error loading image from camera: $currentPhotoPath", e)
                    Toast.makeText(this, "Failed to load image from camera.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("CameraLauncher", "currentPhotoPath is empty after taking photo.")
                Toast.makeText(this, "Error getting photo path.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //gallerylauncher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri
                try {
                    photoPreview.setImageURI(uri)
                    photoPreview.visibility = View.VISIBLE
                    photoLabel.text = getFileNameFromUri(uri)
                    attachPhotoButton.setImageResource(R.drawable.ic_placeholder)
                } catch (e: Exception) {
                    Log.e("GalleryLauncher", "Error loading image from gallery: $uri", e)
                    Toast.makeText(this, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show()
                }
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