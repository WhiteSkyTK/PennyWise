package com.tk.pennywise

import android.os.Bundle
import android.view.View // Import View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI Elements
    private lateinit var backButton: ImageButton
    private lateinit var feedbackInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var submitBtn: MaterialButton
    private lateinit var submitFeedbackProgressBar: ProgressBar // Declare ProgressBar

    private var originalSubmitButtonText: String = "" // To store original text

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        setContentView(R.layout.activity_feedback)
        supportActionBar?.hide()

        FirebaseApp.initializeApp(this)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize UI Elements
        backButton = findViewById(R.id.backButton)
        feedbackInput = findViewById(R.id.feedbackInput)
        emailInput = findViewById(R.id.emailInput)
        submitBtn = findViewById(R.id.submitFeedbackBtn)
        submitFeedbackProgressBar = findViewById(R.id.submitFeedbackProgressBar) // Initialize ProgressBar

        originalSubmitButtonText = submitBtn.text.toString() // Store original text

        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        submitBtn.setOnClickListener {
            val feedbackText = feedbackInput.text.toString().trim()
            val emailText = emailInput.text.toString().trim()

            if (feedbackText.isEmpty()) {
                feedbackInput.error = "Feedback cannot be empty" // More direct feedback
                // Toast.makeText(this, "Feedback cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                feedbackInput.error = null // Clear error
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- START LOADING STATE ---
            setSubmitButtonLoadingState(true)

            val userId = currentUser.uid
            val feedback = hashMapOf( // Use HashMap for simpler Firestore data
                "message" to feedbackText,
                "email" to if (emailText.isEmpty()) null else emailText,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp() // Good practice
            )

            db.collection("users")
                .document(userId)
                .collection("feedbacks")
                .add(feedback)
                .addOnSuccessListener {
                    setSubmitButtonLoadingState(false)
                    Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                    feedbackInput.text?.clear()
                    emailInput.text?.clear()
                    feedbackInput.requestFocus() // Optionally focus first field
                }
                .addOnFailureListener { e ->
                    // --- END LOADING STATE (FAILURE) ---
                    setSubmitButtonLoadingState(false)
                    Toast.makeText(this, "Failed to submit: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setSubmitButtonLoadingState(isLoading: Boolean) {
        if (isLoading) {
            submitBtn.isEnabled = false
            submitBtn.text = "" // Clear text to show ProgressBar
            submitFeedbackProgressBar.visibility = View.VISIBLE
        } else {
            submitBtn.isEnabled = true
            submitFeedbackProgressBar.visibility = View.GONE
            submitBtn.text = originalSubmitButtonText // Reset to original text
        }
    }
}
