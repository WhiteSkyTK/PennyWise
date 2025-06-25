package com.example.pennywise

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var userId: String
    private lateinit var transactionId: String  // Use String for Firestore doc ID

    private var amount: Double = 0.0
    private lateinit var type: String
    private lateinit var category: String
    private var description: String? = null
    private var photoPath: String? = null
    private var originalDate: Long = 0L
    private lateinit var originalStartTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
        db.firestoreSettings = settings
        setContentView(R.layout.activity_transaction_detail)
        supportActionBar?.hide() //action bar

        transactionId = intent?.getStringExtra("transaction_id") ?: ""
        if (transactionId.isEmpty()) {
            finish() // No transaction id, close
            return
        }

        // Initialize from intent
        amount = intent.getDoubleExtra("amount", 0.0)
        type = intent.getStringExtra("type") ?: "Unknown"
        category = intent.getStringExtra("category") ?: "Unknown"
        description = intent.getStringExtra("description")
        photoPath = intent.getStringExtra("photoPath")
        originalDate = intent.getLongExtra("date", 0L)
        originalStartTime = intent.getStringExtra("startTime") ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        val amountText = findViewById<TextView>(R.id.amountText)
        val typeText = findViewById<TextView>(R.id.typeText)
        val categoryText = findViewById<TextView>(R.id.categoryText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        val dateText = findViewById<TextView>(R.id.dateText)
        val timeText = findViewById<TextView>(R.id.timeText)
        val photoView = findViewById<ImageView>(R.id.transactionPhoto)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Use these to populate the screen
        Log.d("TransactionDetail", "ID: $transactionId | Amount: $amount | Type: $type")
        // Type formatting
        typeText.text = "Type: $type"
        if (type.equals("Expense", ignoreCase = true)) {
            amountText.setTextColor(resources.getColor(R.color.expense_red, theme))
            amountText.text = "-R%.2f".format(amount)
        } else {
            amountText.setTextColor(resources.getColor(R.color.green_400, theme))
            amountText.text = "R%.2f".format(amount)
        }
        categoryText.text = "Category: $category"
        descriptionText.text = "Description: ${description ?: "No description"}"
        dateText.text = "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(originalDate))}"
        timeText.text = "Time: $originalStartTime"

        // Photo (conditionally visible)
        if (!photoPath.isNullOrEmpty()) {
            photoView.setImageURI(Uri.parse(photoPath))
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
        editIntent.putExtra("photoUri", photoPath)
        editIntent.putExtra("userId", userId)
        startActivity(editIntent)
    }

    private fun confirmAndDeleteTransaction() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d("DeleteDebug", "Deleting transaction ID: $transactionId")
                        // Assuming you have a Firestore collection named "transactions"
                        firestore.collection("users")
                            .document(userId)
                            .collection("transactions")
                            .document(transactionId)
                            .delete()
                            .await()

                        withContext(Dispatchers.Main) {
                            val msg = "Deleted transaction successfully"
                            Log.d("DeleteToast", msg)
                            Toast.makeText(this@TransactionDetailActivity, msg, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        val errorMsg = "Error deleting transaction: ${e.message}"
                        Log.e("DeleteError", errorMsg, e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TransactionDetailActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}