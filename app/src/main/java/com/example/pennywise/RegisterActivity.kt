package com.example.pennywise

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
}