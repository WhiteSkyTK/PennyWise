package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch
import java.security.MessageDigest
import android.content.Context

class ProfileActivity : AppCompatActivity() {
    //declartion
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var textMessage: TextView
    private lateinit var originalEmail: String
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        // Make status bar transparent
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

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
        textMessage = findViewById(R.id.textMessage)
        originalEmail = intent.getStringExtra("user_email") ?: ""
        Log.d("ProfileActivity", "Original Email: $originalEmail")

        // Toggle password visibility
        iconTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Back button logic
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load user data into fields
        lifecycleScope.launch {
            val userDao = AppDatabase.getDatabase(applicationContext).userDao()
            val user = userDao.getUserByEmail(originalEmail)
            Log.d("ProfileActivity", "User fetched by email ($originalEmail): $user")

            val allUsers = userDao.getAllUsers()  // Add this method temporarily to your UserDao
            Log.d("ProfileActivity", "All users in DB: $allUsers")

            user?.let {
                runOnUiThread {
                    editEmail.setText(it.email)
                    editPassword.setText("") // Don't show hashed password
                }
            }
        }

        findViewById<Button>(R.id.buttonUpdate).setOnClickListener {
            textMessage.visibility = TextView.GONE // Clear previous message
            val newEmail = editEmail.text.toString().trim()
            val newPassword = editPassword.text.toString().trim()

            // Validate email
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                editEmail.error = "Please enter a valid email address"
                editEmail.requestFocus()
                return@setOnClickListener
            } else {
                editEmail.error = null
            }

            // Validate password length (allow empty password if user wants no change)
            if (newPassword.isNotEmpty() && newPassword.length < 6) {
                editPassword.error = "Password must be at least 6 characters"
                editPassword.requestFocus()
                return@setOnClickListener
            } else {
                editPassword.error = null
            }

            val hashedPassword = if (newPassword.isNotEmpty()) hashPassword(newPassword) else null

            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(applicationContext).userDao()
                val currentUser = userDao.getUserByEmail(originalEmail)

                if (currentUser != null) {
                    currentUser.email = newEmail
                    if (hashedPassword != null) currentUser.password = hashedPassword

                    userDao.updateUser(currentUser)

                    // Update SharedPreferences with new email
                    val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("loggedInUserEmail", newEmail)
                        apply()
                    }

                    // Update local originalEmail too
                    originalEmail = newEmail

                    runOnUiThread {
                        textMessage.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        textMessage.text = "Profile updated successfully!"
                        textMessage.visibility = TextView.VISIBLE
                    }
                } else {
                    runOnUiThread {
                        textMessage.setTextColor(resources.getColor(android.R.color.holo_red_light))
                        textMessage.text = "Failed to update profile."
                        textMessage.visibility = TextView.VISIBLE
                    }
                }
            }
        }

        findViewById<Button>(R.id.buttonDeleteAccount).setOnClickListener {
            textMessage.visibility = TextView.GONE

            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(applicationContext).userDao()
                val user = userDao.getUserByEmail(originalEmail)

                if (user != null) {
                    userDao.deleteUser(user)

                    // Clear saved email and logged-in flag from SharedPreferences here:
                    val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        remove("loggedInUserEmail")
                        putBoolean("logged_in", false)
                        apply()
                    }

                    runOnUiThread {
                        textMessage.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        textMessage.text = "Account deleted."
                        textMessage.visibility = TextView.VISIBLE

                        // Redirect to RegisterActivity after short delay
                        textMessage.postDelayed({
                            val intent = Intent(this@ProfileActivity, RegisterActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }, 1000)
                    }
                } else {
                    runOnUiThread {
                        textMessage.setTextColor(resources.getColor(android.R.color.holo_red_light))
                        textMessage.text = "Account deletion failed."
                        textMessage.visibility = TextView.VISIBLE
                    }
                }
            }
        }
    }

    //show hide password
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

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}