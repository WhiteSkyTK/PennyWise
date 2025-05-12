package com.example.pennywise

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)
        supportActionBar?.hide() //action bar

        //declartion
        val amount = intent.getDoubleExtra("amount", 0.0)
        val type = intent.getStringExtra("type") ?: "Unknown"
        val category = intent.getStringExtra("category") ?: "Unknown"
        val description = intent.getStringExtra("description")
        val date = intent.getLongExtra("date", 0L)
        val time = intent.getStringExtra("startTime") ?: "N/A"
        val photoUri = intent.getStringExtra("photoUri")

        val amountText = findViewById<TextView>(R.id.amountText)
        val typeText = findViewById<TextView>(R.id.typeText)
        val categoryText = findViewById<TextView>(R.id.categoryText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        val dateText = findViewById<TextView>(R.id.dateText)
        val timeText = findViewById<TextView>(R.id.timeText)
        val photoView = findViewById<ImageView>(R.id.transactionPhoto)
        val backButton = findViewById<ImageButton>(R.id.backButton)

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
        dateText.text = "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date))}"
        timeText.text = "Time: $time"

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
    }
}