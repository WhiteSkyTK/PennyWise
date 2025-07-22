package com.tk.pennywise

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat // Added for color resources
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        setContentView(R.layout.activity_transaction_detail)
        supportActionBar?.hide() //action bar

        transactionId = intent?.getStringExtra("transaction_id") ?: ""
        if (transactionId.isEmpty()) {
            Toast.makeText(this, "Transaction ID missing.", Toast.LENGTH_SHORT).show()
            finish()
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

        // --- Find Views from the new layout ---
        val amountTextView = findViewById<TextView>(R.id.amountText) // In Amount Card
        val typeTextView = findViewById<TextView>(R.id.typeText)     // In Amount Card
        val categoryTextView = findViewById<TextView>(R.id.categoryText) // In Details Card
        val dateTextView = findViewById<TextView>(R.id.dateText)         // In Details Card
        val timeTextView = findViewById<TextView>(R.id.timeText)         // In Details Card

        // Description related views
        val descriptionLayout = findViewById<LinearLayout>(R.id.descriptionLayout)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionText) // Inside descriptionLayout
        val descriptionDivider = findViewById<View>(R.id.descriptionDivider)

        // Photo related views
        val transactionPhotoCard = findViewById<MaterialCardView>(R.id.transactionPhotoCard)
        val transactionPhotoImageView = findViewById<ImageView>(R.id.transactionPhoto) // Inside transactionPhotoCard

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val optionsIcon = findViewById<ImageButton>(R.id.optionsIcon) // Changed to ImageButton if you updated XML

        // --- Populate Views ---
        Log.d("TransactionDetail", "ID: $transactionId | Amount: $amount | Type: $type")

        // Type formatting
        typeTextView.text = type // Just the type, e.g., "Expense" or "Income"
        if (type.equals("Expense", ignoreCase = true)) {
            amountTextView.text = "-R%.2f".format(amount)
            // Assuming you have these colors defined in your colors.xml
            amountTextView.setTextColor(ContextCompat.getColor(this, R.color.expense_red))
        } else { // Assuming "Income" or other positive types
            amountTextView.text = "R%.2f".format(amount)
            amountTextView.setTextColor(ContextCompat.getColor(this, R.color.income_green)) // Use a green color
        }

        // Details Card
        categoryTextView.text = category // Just the category name
        dateTextView.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(originalDate))
        timeTextView.text = originalStartTime

        // Conditional Description
        if (description.isNullOrEmpty() || description == "No description") {
            descriptionLayout.visibility = View.GONE
            descriptionDivider.visibility = View.GONE
        } else {
            descriptionTextView.text = description
            descriptionLayout.visibility = View.VISIBLE
            descriptionDivider.visibility = View.VISIBLE
        }

        // Conditional Photo Preview
        if (photoPath.isNullOrEmpty()) {
            transactionPhotoCard.visibility = View.GONE
        } else {
            try {
                transactionPhotoImageView.setImageURI(Uri.parse(photoPath))
                transactionPhotoCard.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e("TransactionDetail", "Error loading image URI: $photoPath", e)
                transactionPhotoCard.visibility = View.GONE // Hide if URI is invalid
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        optionsIcon.setOnClickListener { view ->
            val popupMenu = PopupMenu(this@TransactionDetailActivity, view)
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
        val editIntent = Intent(this, Activityaddentry::class.java).apply {
            putExtra("isEdit", true)
            putExtra("transactionId", transactionId)
            putExtra("amount", amount)
            putExtra("type", type)
            putExtra("category", category)
            putExtra("date", originalDate)
            putExtra("startTime", originalStartTime)
            putExtra("description", description)
            putExtra("photoUri", photoPath) // Ensure Activityaddentry can handle this key
            putExtra("userId", userId) // Pass userId if needed by Activityaddentry
        }
        editTransactionLauncher.launch(editIntent)
    }

    private fun confirmAndDeleteTransaction() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d("DeleteDebug", "Deleting transaction ID: $transactionId")
                        if (userId.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TransactionDetailActivity, "User ID is missing. Cannot delete.", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }
                        firestore.collection("users")
                            .document(userId)
                            .collection("transactions")
                            .document(transactionId)
                            .delete()
                            .await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TransactionDetailActivity, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                            val resultIntent = Intent()
                            resultIntent.putExtra("transaction_deleted", true)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteError", "Error deleting transaction: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TransactionDetailActivity, "Error deleting transaction: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Activity Result Launcher for editing
    private val editTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Data might have changed, so we need to indicate this to the calling activity (MainActivity)
            val mainActivityResultIntent = Intent()
            mainActivityResultIntent.putExtra("needs_refresh", true) // Generic refresh flag

            // If ActivityAddEntry sends back specific month info, pass it along
            result.data?.getStringExtra("transaction_changed_month")?.let { month ->
                mainActivityResultIntent.putExtra("transaction_changed_month", month)
            }
            // If ActivityAddEntry sends back the updated transaction details, you could reload them here
            // or simply finish and let MainActivity handle the refresh.
            // For simplicity, we'll set result and finish.
            setResult(RESULT_OK, mainActivityResultIntent)
            finish()
        } else {
            Log.d("TransactionDetail", "Edit cancelled or failed. Result code: ${result.resultCode}")
            // Optionally, if you want to refresh the detail view even if the edit was just "backed out of"
            // without saving, you could re-fetch the data here. But typically, if no save, no refresh.
        }
    }
}
