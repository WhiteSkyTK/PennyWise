package com.example.pennywise

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
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch

class activity_add_category : AppCompatActivity() {

    //later decleartion
    private lateinit var categoryDao: com.example.pennywise.CategoryDao
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)

        //hide status bar
        supportActionBar?.hide()

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

        // Create category
        createCategoryBtn.setOnClickListener {
            val rawName = categoryNameInput.text.toString().trim()
            val selectedType = categoryTypeSpinner.selectedItem.toString()

            if (rawName.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedType == "Please select a type") {
                Toast.makeText(this, "Please select a category type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formattedName = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val normalizedType = selectedType.lowercase()

            val newCategory = Category(
                name = formattedName,
                type = normalizedType,
            )

            lifecycleScope.launch {
                categoryDao.insert(newCategory)
                Toast.makeText(this@activity_add_category, "Category saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Back navigation
        backButton.setOnClickListener { finish() }
    }
}