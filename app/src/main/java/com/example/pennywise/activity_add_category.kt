package com.example.pennywise

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch

class activity_add_category : AppCompatActivity() {

    private lateinit var categoryDao: com.example.pennywise.CategoryDao
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)

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
        val saveCategoryBtn = findViewById<ImageButton>(R.id.saveCategoryBtn)
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)

        // Create category
        createCategoryBtn.setOnClickListener {
            val name = categoryNameInput.text.toString().trim()

            if (name.isNotEmpty()) {
                if (userEmail != null) {
                    val newCategory = Category(
                        name = name,
                        type = "Expense",        // or "Income" if this is an income category
                    )

                    lifecycleScope.launch {
                        categoryDao.insert(newCategory)
                        Toast.makeText(this@activity_add_category, "Add_Category saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }


        // Back navigation
        backButton.setOnClickListener { finish() }
        saveCategoryBtn.setOnClickListener { createCategoryBtn.performClick() }
    }
}
