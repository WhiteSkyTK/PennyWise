package com.example.pennywise

import android.R.attr.category
import android.R.attr.description
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var userEmail: String
    private var originalDate: Long = 0L
    private lateinit var originalStartTime: String
    private var transactionId: Long = -1
    private var amount: Double = 0.0
    private lateinit var type: String
    private lateinit var category: String
    private var description: String? = null
    private var photoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)
        supportActionBar?.hide() //action bar

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        transactionId = intent?.getLongExtra("transaction_id", -1) ?: -1
        if (transactionId == -1L) {
            finish() // or show error
            return
        }
        db = AppDatabase.getDatabase(this)

        // Initialize from intent
        amount = intent.getDoubleExtra("amount", 0.0)
        type = intent.getStringExtra("type") ?: "Unknown"
        category = intent.getStringExtra("category") ?: "Unknown"
        description = intent.getStringExtra("description")
        photoUri = intent.getStringExtra("photoUri")
        originalDate = intent.getLongExtra("date", 0L)
        originalStartTime = intent.getStringExtra("startTime") ?: ""
        userEmail = intent.getStringExtra("userEmail") ?: ""

        val amountText = findViewById<TextView>(R.id.amountText)
        val typeText = findViewById<TextView>(R.id.typeText)
        val categoryText = findViewById<TextView>(R.id.categoryText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        val dateText = findViewById<TextView>(R.id.dateText)
        val timeText = findViewById<TextView>(R.id.timeText)
        val photoView = findViewById<ImageView>(R.id.transactionPhoto)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        userEmail = intent.getStringExtra("userEmail") ?: ""
        originalDate = intent.getLongExtra("date", 0L)
        originalStartTime = intent.getStringExtra("startTime") ?: ""

        // Type formatting
        typeText.text = "Type: $type"

        // Format amount based on type
        if (type.equals("Expense", ignoreCase = true)) {
            amountText.setTextColor(resources.getColor(R.color.expense_red, theme))
            amountText.text = "-R%.2f".format(amount)
        } else {
            amountText.setTextColor(resources.getColor(R.color.green_400, theme))
            amountText.text = "R%.2f".format(amount)
        }

        // Other fields
        categoryText.text = "Category: $category"
        descriptionText.text = "Description: ${description ?: "No description"}"
        dateText.text =
            "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(originalDate))}"
        timeText.text = "Time: $originalStartTime"

        // Photo (conditionally visible)
        if (!photoUri.isNullOrEmpty()) {
            photoView.setImageURI(Uri.parse(photoUri))
            photoView.visibility = ImageView.VISIBLE
        } else {
            photoView.visibility = ImageView.GONE
        }

        backButton.setOnClickListener {
            finish()
        }

        val optionsIcon = findViewById<ImageView>(R.id.optionsIcon)
        optionsIcon.setOnClickListener { view ->
            val popupMenu = android.widget.PopupMenu(this@TransactionDetailActivity, view)
            popupMenu.menuInflater.inflate(R.menu.transaction_options_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit -> {
                        launchEditTransaction()
                        true
                    }

                    R.id.menu_delete -> {
                        confirmAndDeleteTransaction()
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun launchEditTransaction() {
        val editIntent = Intent(this, Activityaddentry::class.java)
        editIntent.putExtra("isEdit", true)
        editIntent.putExtra("transactionId", transactionId)
        editIntent.putExtra("amount", amount)
        editIntent.putExtra("type", type)
        editIntent.putExtra("category", category)
        editIntent.putExtra("date", originalDate)
        editIntent.putExtra("startTime", originalStartTime)
        editIntent.putExtra("description", description)
        editIntent.putExtra("photoUri", photoUri)
        editIntent.putExtra("userEmail", userEmail)
        startActivity(editIntent)
    }

    private fun confirmAndDeleteTransaction() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.transactionDao().deleteTransactionById(transactionId)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TransactionDetailActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        finish() // Or navigate to main
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}