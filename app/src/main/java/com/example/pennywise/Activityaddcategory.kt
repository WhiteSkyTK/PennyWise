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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class Activityaddcategory : AppCompatActivity() {
    private var categoryId: String? = null  // Firestore doc ID
    private val db = FirebaseFirestore.getInstance()
    private lateinit var categoryCollection: CollectionReference

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
        categoryId = intent.getStringExtra("category_id")
        if (categoryId != null) {
            // Load category from Firestore
            categoryCollection.document(categoryId!!).get().addOnSuccessListener { doc ->
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
                    createCategoryBtn.text = "Save Changes"
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

            val newCategory = Category(
                name = formattedName,
                type = normalizedType
            )

            if (categoryId != null) {
                // Update category
                categoryCollection.document(categoryId!!).set(newCategory).addOnSuccessListener {
                    returnToCaller(formattedName, normalizedType)
                }
            } else {
                // Check for duplicates
                categoryCollection
                    .whereEqualTo("name", formattedName)
                    .whereEqualTo("type", normalizedType)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            categoryNameError.text = "This category already exists."
                            categoryNameError.visibility = View.VISIBLE
                        } else {
                            val newDoc = categoryCollection.document()
                            newCategory.id = newDoc.id
                            newDoc.set(newCategory).addOnSuccessListener {
                                returnToCaller(formattedName, normalizedType)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to check duplicates.", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Back navigation
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun returnToCaller(name: String, type: String) {
        if (intent.getBooleanExtra("fromAddEntry", false)) {
            val resultIntent = Intent().apply {
                putExtra("newCategory", name)
                putExtra("newCategoryType", type)
            }
            setResult(RESULT_OK, resultIntent)
        }
        AddCategory.shouldRefreshOnResume = true
        setResult(RESULT_OK)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}