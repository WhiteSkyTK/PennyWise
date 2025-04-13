package com.example.pennywise

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {

    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var backButton: ImageButton
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Edge-to-edge layout support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileLayout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Views
        editEmail = findViewById(R.id.editEmail)
        editPassword = findViewById(R.id.editPassword)
        iconTogglePassword = findViewById(R.id.iconTogglePassword)
        backButton = findViewById(R.id.backButton)

        // Toggle password visibility
        iconTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Back button logic
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Placeholder listeners
        findViewById<Button>(R.id.buttonUpdate).setOnClickListener {
            Toast.makeText(this, "Update clicked", Toast.LENGTH_SHORT).show()
            // Add update logic here
        }

        findViewById<Button>(R.id.buttonSignOut).setOnClickListener {
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
            // Add sign out logic here
        }

        findViewById<Button>(R.id.buttonDeleteAccount).setOnClickListener {
            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
            // Add delete account logic here
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            iconTogglePassword.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye))
        } else {
            // Show password
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            iconTogglePassword.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye_off))
        }
        // Move cursor to the end
        editPassword.setSelection(editPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }
}
