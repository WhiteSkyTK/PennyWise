package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Activity_Login_Resgister : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_resgister)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailInput = findViewById(R.id.editTextTextEmailAddress)
        passwordInput = findViewById(R.id.editTextTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            var isValid = true

            // Validate email
            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email"
                isValid = false
            } else {
                emailInput.error = null
            }

            // Validate password
            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                isValid = false
            } else if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                isValid = false
            } else {
                passwordInput.error = null
            }

            if (isValid) {
                // TODO: Replace with actual database login logic
                // For now we simulate a successful login
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
