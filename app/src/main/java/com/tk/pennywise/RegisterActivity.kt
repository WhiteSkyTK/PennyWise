package com.tk.pennywise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    //declartions
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editPasswordConfirm: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var iconTogglePassword2: ImageView
    private var isPasswordVisible = false
    private lateinit var backButton: ImageButton
    private lateinit var registerBtn: Button // Declare at class level
    private lateinit var lottieLoadingView: LottieAnimationView // Declare Lottie view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        setContentView(R.layout.activity_register)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        //app setting layout
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
        registerBtn = findViewById(R.id.buttonRegisterConfirm) // Initialize class level button
        lottieLoadingView = findViewById(R.id.lottieLoadingViewRegister) // Initialize Lottie view

        // Listeners for password visibility icons and inputs
        setupPasswordToggleListeners()

        // Back button logic
        backButton.setOnClickListener {
            val intent = Intent(this, ActivityLoginResgister::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        registerBtn.setOnClickListener {
            // Use class members directly
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val confirmPassword = editPasswordConfirm.text.toString().trim()

            if (validateInputs(email, password, confirmPassword)) {
                startLoadingAnimation() // Start loading animation
                performRegistration(email, password)
            }
        }
    }

    private fun setupPasswordToggleListeners() {
        editPassword.setOnFocusChangeListener { _, hasFocus ->
            iconTogglePassword.visibility = if (hasFocus) ImageView.VISIBLE else ImageView.INVISIBLE
            if (hasFocus) iconTogglePassword2.visibility = ImageView.INVISIBLE
        }

        editPasswordConfirm.setOnFocusChangeListener { _, hasFocus ->
            iconTogglePassword2.visibility = if (hasFocus) ImageView.VISIBLE else ImageView.INVISIBLE
            if (hasFocus) iconTogglePassword.visibility = ImageView.INVISIBLE
        }

        editPassword.setOnClickListener {
            iconTogglePassword.visibility = ImageView.VISIBLE
            iconTogglePassword2.visibility = ImageView.INVISIBLE
        }

        editPasswordConfirm.setOnClickListener {
            iconTogglePassword2.visibility = ImageView.VISIBLE
            iconTogglePassword.visibility = ImageView.INVISIBLE
        }

        iconTogglePassword.setOnClickListener {
            togglePasswordVisibility(editPassword, iconTogglePassword, true)
        }

        iconTogglePassword2.setOnClickListener {
            togglePasswordVisibility(editPasswordConfirm, iconTogglePassword2, false) // Use a different flag or manage visibility better
        }
    }

    private fun validateInputs(email: String, pass: String, confirmPass: String): Boolean {
        var isValid = true
        // Email Validation
        if (email.isEmpty()) {
            editEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Invalid email format"
            isValid = false
        } else {
            editEmail.error = null
        }

        // Password Validation
        if (pass.isEmpty()) {
            editPassword.error = "Password is required"
            isValid = false
        } else if (pass.length < 6) {
            editPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            editPassword.error = null
        }

        // Confirm Password Validation
        if (confirmPass.isEmpty()) {
            editPasswordConfirm.error = "Please confirm your password"
            isValid = false
        } else if (pass != confirmPass) {
            editPasswordConfirm.error = "Passwords do not match"
            isValid = false
        } else {
            editPasswordConfirm.error = null
        }
        return isValid
    }

    private fun performRegistration(email: String, pass: String) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result.user
                    if (firebaseUser != null) {
                        val userId = firebaseUser.uid
                        val user = mapOf("email" to email, "uid" to userId) // Good to store UID directly

                        firestore.collection("users").document(userId).set(user)
                            .addOnSuccessListener {
                                Log.d("RegisterActivity", "User profile created in Firestore.")
                                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("logged_in", true)
                                    .putString("loggedInUserEmail", email)
                                    .putString("loggedInUserId", userId)
                                    .apply()

                                lifecycleScope.launch {
                                    PreloadedCategories.preloadUserCategories(userId)
                                    stopLoadingAnimation() // Stop animation before navigating
                                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                    finish()
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                }
                            }
                            .addOnFailureListener { e ->
                                stopLoadingAnimation() // Stop animation on failure
                                Log.e("Firestore", "Failed to save user", e)
                                Toast.makeText(this, "Registration successful, but failed to save profile. Please try logging in.", Toast.LENGTH_LONG).show()
                                // Optionally, navigate to login or main activity anyway if auth succeeded
                                // startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                // finish()
                            }
                    } else {
                        stopLoadingAnimation() // Stop animation
                        Log.e("FirebaseAuth", "User is null even though registration succeeded")
                        Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    stopLoadingAnimation() // Stop animation on failure
                    val error = task.exception?.message ?: "Unknown registration error"
                    Log.e("FirebaseAuth", "Registration failed: $error", task.exception)
                    Toast.makeText(this, "Registration failed: $error", Toast.LENGTH_LONG).show()
                    // Set error on the relevant field if possible, e.g., if it's an email already in use
                    if (error.contains("email address is already in use", ignoreCase = true)) {
                        editEmail.error = "This email address is already in use."
                    } else {
                        editEmail.error = " " // Generic error indicator for email
                    }
                }
            }
    }

    //show hide password icon
    private fun togglePasswordVisibility(editText: EditText, icon: ImageView, isFirstField: Boolean) {
        // This is a simplified toggle. You might want to use separate boolean flags
        // if `isPasswordVisible` is shared and causes issues.
        // For now, let's assume `isPasswordVisible` can be toggled independently for each call.

        val currentInputType = editText.inputType
        if ((currentInputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            icon.setImageResource(R.drawable.ic_eye)
        } else {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            icon.setImageResource(R.drawable.ic_eye_off)
        }
        editText.setSelection(editText.text.length)
        // If you intend for isPasswordVisible to be specific to this field, manage it differently
        // isPasswordVisible = !isPasswordVisible // This will toggle the global one
    }

    private fun startLoadingAnimation() {
        registerBtn.isEnabled = false
        registerBtn.alpha = 0.5f
        editEmail.isEnabled = false
        editPassword.isEnabled = false
        editPasswordConfirm.isEnabled = false
        backButton.isEnabled = false // Disable back button during loading too
        backButton.alpha = 0.5f

        lottieLoadingView.visibility = View.VISIBLE
        lottieLoadingView.playAnimation()
    }

    private fun stopLoadingAnimation() {
        registerBtn.isEnabled = true
        registerBtn.alpha = 1.0f
        editEmail.isEnabled = true
        editPassword.isEnabled = true
        editPasswordConfirm.isEnabled = true
        backButton.isEnabled = true
        backButton.alpha = 1.0f

        lottieLoadingView.cancelAnimation()
        lottieLoadingView.visibility = View.GONE
    }
}