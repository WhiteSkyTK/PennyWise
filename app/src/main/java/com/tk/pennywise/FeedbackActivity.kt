package com.tk.pennywise


import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        setContentView(R.layout.activity_feedback)
        supportActionBar?.hide()

        FirebaseApp.initializeApp(this)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // UI Elements
        val backButton: ImageButton = findViewById(R.id.backButton)
        val feedbackInput: TextInputEditText = findViewById(R.id.feedbackInput)
        val emailInput: TextInputEditText = findViewById(R.id.emailInput)
        val submitBtn: MaterialButton = findViewById(R.id.submitFeedbackBtn)

        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        submitBtn.setOnClickListener {
            val feedbackText = feedbackInput.text.toString().trim()
            val emailText = emailInput.text.toString().trim()

            if (feedbackText.isEmpty()) {
                Toast.makeText(this, "Feedback cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = currentUser.uid
            val feedback = Feedback(
                message = feedbackText,
                email = if (emailText.isEmpty()) null else emailText
            )

            db.collection("users")
                .document(userId)
                .collection("feedbacks")
                .add(feedback)
                .addOnSuccessListener {
                    Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                    feedbackInput.text?.clear()
                    emailInput.text?.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
