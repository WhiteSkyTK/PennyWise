package com.example.pennywise

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editPasswordConfirm: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var iconTogglePassword2: ImageView
    private var isPasswordVisible = false
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        iconTogglePassword = findViewById(R.id.iconTogglePassword)
        iconTogglePassword2 = findViewById(R.id.iconTogglePassword2)
        backButton = findViewById(R.id.backButton)
        editEmail = findViewById(R.id.editTextEmail)
        editPassword = findViewById(R.id.editTextPassword)
        editPasswordConfirm = findViewById(R.id.editTextConfirmPassword)

        editPassword.setOnFocusChangeListener { _, hasFocus ->
            iconTogglePassword.visibility = if (hasFocus) ImageView.VISIBLE else ImageView.INVISIBLE
            iconTogglePassword2.visibility = ImageView.INVISIBLE
        }

        editPasswordConfirm.setOnFocusChangeListener { _, hasFocus ->
            iconTogglePassword2.visibility = if (hasFocus) ImageView.VISIBLE else ImageView.INVISIBLE
            iconTogglePassword.visibility = ImageView.INVISIBLE
        }

        editPassword.setOnClickListener {
            iconTogglePassword.visibility = ImageView.VISIBLE
            iconTogglePassword2.visibility = ImageView.INVISIBLE
        }

        editPasswordConfirm.setOnClickListener {
            iconTogglePassword2.visibility = ImageView.VISIBLE
            iconTogglePassword.visibility = ImageView.INVISIBLE
        }


        // Toggle password visibility
        iconTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        iconTogglePassword2.setOnClickListener {
            togglePasswordVisibility2()
        }

        // Back button logic
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val confirmPasswordInput = findViewById<EditText>(R.id.editTextConfirmPassword)
        val registerBtn = findViewById<Button>(R.id.buttonRegisterConfirm)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true

            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Invalid email format"
                isValid = false
            } else {
                emailInput.error = null
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                isValid = false
            } else if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                isValid = false
            } else {
                passwordInput.error = null
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordInput.error = "Please confirm your password"
                isValid = false
            } else if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                isValid = false
            } else {
                confirmPasswordInput.error = null
            }

            if (isValid) {
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            iconTogglePassword.setImageResource(R.drawable.ic_eye)
        } else {
            // Show
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            iconTogglePassword.setImageResource(R.drawable.ic_eye_off)
        }
        editPassword.setSelection(editPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun togglePasswordVisibility2() {
        if (isPasswordVisible) {
            // Hide
            editPasswordConfirm.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            iconTogglePassword2.setImageResource(R.drawable.ic_eye)
        } else {
            // Show
            editPasswordConfirm.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            iconTogglePassword2.setImageResource(R.drawable.ic_eye_off)
        }
        editPasswordConfirm.setSelection(editPasswordConfirm.text.length)
        isPasswordVisible = !isPasswordVisible
    }

}