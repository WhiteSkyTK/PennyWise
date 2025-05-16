package com.example.pennywise

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch

class activity_add_category : AppCompatActivity() {

    //later decleartion
    private lateinit var categoryDao: com.example.pennywise.CategoryDao
    private lateinit var db: AppDatabase

    private var categoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)

        //hide status bar
        supportActionBar?.hide()

        //Layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize DB + DAO
        db = AppDatabase.getDatabase(this)
        categoryDao = db.categoryDao()

        // UI references
        val categoryNameInput = findViewById<EditText>(R.id.categoryNameInput)
        val createCategoryBtn = findViewById<Button>(R.id.createCategoryBtn)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val categoryTypeSpinner = findViewById<Spinner>(R.id.categoryTypeSpinner)
        val categoryNameError = findViewById<TextView>(R.id.categoryNameError)
        val categoryTypeError = findViewById<TextView>(R.id.categoryTypeError)

        // Setup the spinner
        val typeOptions = listOf("Please select a type", "Expense", "Income", "Other")
        val spinnerAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            typeOptions
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0 // Disable the first item
            }

            //drop down function
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

        //spinner logic
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryTypeSpinner.adapter = spinnerAdapter
        categoryTypeSpinner.setSelection(0)

        // Check if this is Edit mode by seeing if intent has category ID
        categoryId = intent.getIntExtra("category_id", -1).takeIf { it != -1 }

        if (categoryId != null) {
            // Load category data and prefill UI
            lifecycleScope.launch {
                val category = categoryDao.getCategoryById(categoryId!!)
                if (category != null) {
                    categoryNameInput.setText(category.name)
                    // Set spinner selection based on type (e.g. expense=1, income=2, other=3)
                    val spinnerPosition = when (category.type.lowercase()) {
                        "expense" -> 1
                        "income" -> 2
                        "other" -> 3
                        else -> 0
                    }
                    categoryTypeSpinner.setSelection(spinnerPosition)
                    createCategoryBtn.text = "Save Changes"  // Change button text for clarity
                }
            }
        }

        // Create category
        createCategoryBtn.setOnClickListener {
            val rawName = categoryNameInput.text.toString().trim()
            val selectedType = categoryTypeSpinner.selectedItem.toString()

            var valid = true
            categoryNameError.visibility = View.GONE
            categoryTypeError.visibility = View.GONE

            if (rawName.isEmpty()) {
                categoryNameError.text = "Please enter a category name"
                categoryNameError.visibility = View.VISIBLE
                valid = false
            }

            if (selectedType == "Please select a type") {
                categoryTypeError.text = "Please select a category type"
                categoryTypeError.visibility = View.VISIBLE
                valid = false
            }

            if (!valid) return@setOnClickListener

            val formattedName = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val normalizedType = selectedType.lowercase()

            lifecycleScope.launch {
                if (categoryId != null) {
                    // Edit mode – update existing category
                    val existingCategory = categoryDao.getCategoryById(categoryId!!)
                    if (existingCategory != null) {
                        val updatedCategory = existingCategory.copy(
                            name = formattedName,
                            type = normalizedType
                        )
                        categoryDao.update(updatedCategory)
                    }
                } else {
                    // New category – insert
                    val newCategory = Category(name = formattedName, type = normalizedType)
                    categoryDao.insert(newCategory)
                }

                // Return result if called from AddEntry
                if (intent.getBooleanExtra("fromAddEntry", false)) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("newCategory", formattedName)
                    resultIntent.putExtra("newCategoryType", normalizedType)
                    setResult(RESULT_OK, resultIntent)
                }

                Add_Category.shouldRefreshOnResume = true
                setResult(RESULT_OK)
                finish()
            }
        }

        // Back navigation
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}