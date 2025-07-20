package com.tk.pennywise

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ProgressBar
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.semantics.text

// import com.airbnb.lottie.LottieAnimationView

class Activityaddcategory : AppCompatActivity() {
    private var categoryId: String? = null  // Firestore doc ID
    private val db = FirebaseFirestore.getInstance()
    private lateinit var categoryCollection: CollectionReference

    // UI References for state management
    private lateinit var createCategoryBtn: Button
    private lateinit var buttonProgressBar: ProgressBar
    private lateinit var categoryNameInput: EditText
    private lateinit var categoryTypeSpinner: Spinner
    // private lateinit var lottieLoadingViewFull: LottieAnimationView // If using full screen Lottie
    // private lateinit var formContainer: View // If using full screen Lottie to hide form

    private var originalButtonText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_add_category)

        //hide status bar
        supportActionBar?.hide()

        // Now safely assign collection
        categoryCollection = db.collection("users").document(userId).collection("categories")


        //Layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI references
        categoryNameInput = findViewById(R.id.categoryNameInput)
        createCategoryBtn = findViewById(R.id.createCategoryBtn)
        buttonProgressBar = findViewById(R.id.buttonProgressBar)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        categoryTypeSpinner = findViewById(R.id.categoryTypeSpinner)
        val categoryNameError = findViewById<TextView>(R.id.categoryNameError)
        val categoryTypeError = findViewById<TextView>(R.id.categoryTypeError)
        // lottieLoadingViewFull = findViewById(R.id.lottieLoadingViewFull) // If using full screen
        // formContainer = findViewById(R.id.formContainer) // If using full screen

        originalButtonText = createCategoryBtn.text.toString() // Store initial button text

        setupSpinner(categoryTypeSpinner)

        categoryId = intent.getStringExtra("category_id")
        if (categoryId != null) {
            originalButtonText = getString(R.string.save_changes) // Or your string resource
            createCategoryBtn.text = originalButtonText
            findViewById<TextView>(R.id.headerText).text = getString(R.string.edit_category) // Or your string resource
            loadCategoryDetails(categoryId!!)
        } else {
            originalButtonText = getString(R.string.create_category) // Or your string resource
            createCategoryBtn.text = originalButtonText
            findViewById<TextView>(R.id.headerText).text = getString(R.string.new_category) // Or your string resource
        }

        createCategoryBtn.setOnClickListener {
            handleCreateOrUpdateCategory()
        }

        backButton.setOnClickListener {
            if (!createCategoryBtn.isEnabled) return@setOnClickListener // Don't allow back if processing
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupSpinner(categoryTypeSpinner: Spinner) {
        val typeOptions = listOf(getString(R.string.please_select_a_type), "Expense", "Income", "Other") // Use string resources
        val spinnerAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            typeOptions
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(
                    if (position == 0) Color.GRAY
                    else if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
                        Color.WHITE
                    else Color.BLACK
                )
                return view
            }
        }
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryTypeSpinner.adapter = spinnerAdapter
        categoryTypeSpinner.setSelection(0)
    }

    private fun loadCategoryDetails(docId: String) {
        // You could show a loading state for the form fields here if desired
        // e.g., disable inputs, show a small progress bar for the form section
        showLoadingState(true, formFieldsOnly = true) // Example for form field loading
        categoryCollection.document(docId).get()
            .addOnSuccessListener { doc ->
                val category = doc.toObject(Category::class.java)
                category?.let {
                    categoryNameInput.setText(it.name)
                    categoryTypeSpinner.setSelection(
                        when (it.type.lowercase()) {
                            "expense" -> 1
                            "income" -> 2
                            "other" -> 3
                            else -> 0
                        }
                    )
                }
                showLoadingState(false, formFieldsOnly = true)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load category details.", Toast.LENGTH_SHORT).show()
                showLoadingState(false, formFieldsOnly = true)
                finish() // Or handle error appropriately
            }
    }

    private fun handleCreateOrUpdateCategory() {
        val rawName = categoryNameInput.text.toString().trim()
        val selectedType = categoryTypeSpinner.selectedItem.toString()

        var valid = true
        findViewById<TextView>(R.id.categoryNameError).visibility = View.GONE
        findViewById<TextView>(R.id.categoryTypeError).visibility = View.GONE

        if (rawName.isEmpty()) {
            findViewById<TextView>(R.id.categoryNameError).text = getString(R.string.please_enter_a_category_name)
            findViewById<TextView>(R.id.categoryNameError).visibility = View.VISIBLE
            valid = false
        }

        if (selectedType == getString(R.string.please_select_a_type)) {
            findViewById<TextView>(R.id.categoryTypeError).text = getString(R.string.please_select_a_category_type)
            findViewById<TextView>(R.id.categoryTypeError).visibility = View.VISIBLE
            valid = false
        }

        if (!valid) return

        showLoadingState(true)

        val formattedName = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val normalizedType = selectedType.lowercase()

        val categoryData = Category(
            name = formattedName,
            type = normalizedType
        )

        if (categoryId != null) { // Update existing category
            categoryCollection.document(categoryId!!).set(categoryData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
                    returnToCaller(formattedName, normalizedType)
                    // showLoadingState(false) // Not strictly needed as we finish
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating category: ${e.message}", Toast.LENGTH_LONG).show()
                    showLoadingState(false)
                }
        } else { // Create new category
            categoryCollection
                .whereEqualTo("name", formattedName)
                .whereEqualTo("type", normalizedType)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        findViewById<TextView>(R.id.categoryNameError).text = "This category already exists."
                        findViewById<TextView>(R.id.categoryNameError).visibility = View.VISIBLE
                        showLoadingState(false)
                    } else {
                        val newDoc = categoryCollection.document()
                        categoryData.id = newDoc.id // Set the auto-generated ID to the object
                        newDoc.set(categoryData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Category created successfully", Toast.LENGTH_SHORT).show()
                                returnToCaller(formattedName, normalizedType)
                                // showLoadingState(false) // Not strictly needed as we finish
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error creating category: ${e.message}", Toast.LENGTH_LONG).show()
                                showLoadingState(false)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to check for duplicates: ${e.message}", Toast.LENGTH_LONG).show()
                    showLoadingState(false)
                }
        }
    }

    private fun showLoadingState(isLoading: Boolean, formFieldsOnly: Boolean = false) {
        if (!formFieldsOnly) {
            if (isLoading) {
                createCategoryBtn.text = "" // Hide text
                buttonProgressBar.visibility = View.VISIBLE
                createCategoryBtn.isEnabled = false
                // If using full screen Lottie:
                // formContainer.visibility = View.GONE
                // lottieLoadingViewFull.visibility = View.VISIBLE
                // lottieLoadingViewFull.playAnimation()
            } else {
                createCategoryBtn.text = originalButtonText // Restore text
                buttonProgressBar.visibility = View.GONE
                createCategoryBtn.isEnabled = true
                // If using full screen Lottie:
                // formContainer.visibility = View.VISIBLE
                // lottieLoadingViewFull.visibility = View.GONE
                // lottieLoadingViewFull.cancelAnimation()
            }
        }

        // Disable/enable form fields during any loading operation (initial or submit)
        categoryNameInput.isEnabled = !isLoading
        categoryTypeSpinner.isEnabled = !isLoading
        findViewById<ImageButton>(R.id.backButton).isEnabled = !isLoading // Also disable back button during processing
    }

    private fun returnToCaller(name: String, type: String) {
        if (intent.getBooleanExtra("fromAddEntry", false)) {
            val resultIntent = Intent().apply {
                putExtra("newCategory", name)
                putExtra("newCategoryType", type)
            }
            setResult(RESULT_OK, resultIntent)
        }
        AddCategory.Companion.shouldRefreshOnResume = true
        setResult(RESULT_OK)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}