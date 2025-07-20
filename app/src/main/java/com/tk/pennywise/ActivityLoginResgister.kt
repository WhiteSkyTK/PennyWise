package com.tk.pennywise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import android.view.View // Import View

import com.airbnb.lottie.LottieAnimationView

class ActivityLoginResgister : AppCompatActivity() {

    //decleartion
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var iconTogglePassword: ImageView
    private lateinit var editPassword:EditText
    private var isPasswordVisible = false
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var lottieLoadingView: LottieAnimationView // Declare Lottie view
    private lateinit var googleSignInButton: Button // Ensure this is declared if not already

    // Google Sign-In
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest>

    companion object {
        private const val TAG = "ActivityLoginRegister"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)
        setContentView(R.layout.activity_login_resgister)
        supportActionBar?.hide()

        //layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //button function
        emailInput = findViewById(R.id.editTextTextEmailAddress)
        passwordInput = findViewById(R.id.editTextTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        editPassword = findViewById(R.id.editTextTextPassword)
        iconTogglePassword = findViewById(R.id.iconTogglePassword)
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn) // Initialize it here
        lottieLoadingView = findViewById(R.id.lottieLoadingView) // Initialize Lottie view

        // --- Initialize Google One- androidx.test.espresso.action.Tap Sign-In ---
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id)) // From google-services.json
                    .setFilterByAuthorizedAccounts(false) // Show all Google accounts
                    .build()
            )
            .setAutoSelectEnabled(true) // Optional: enable auto sign-in
            .build()

        googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                stopLoadingAnimation() // Stop animation regardless of outcome here
                if (result.resultCode == RESULT_OK) {
                    try {
                        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                        val idToken = credential.googleIdToken
                        when {
                            idToken != null -> {
                                // Start loading animation before Firebase auth
                                startLoadingAnimation(listOf(googleSignInButton, loginButton, registerButton))
                                firebaseAuthWithGoogle(idToken)
                            }
                            // ... (rest of your Google Sign-In logic)
                            else -> {
                                Log.e(TAG, "Google Sign-In: No ID token or password!")
                                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: ApiException) {
                        Log.e(TAG, "Google Sign-In failed: ${e.statusCode}", e)
                        Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Google Sign-In cancelled or failed. Result code: ${result.resultCode}")
                }
            }

        googleSignInButton.setOnClickListener {
            // Start loading animation immediately for Google button
            startLoadingAnimation(listOf(googleSignInButton, loginButton, registerButton))
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        // The launcher will handle stopping the animation or continuing it if firebaseAuthWithGoogle starts it again
                        googleSignInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    } catch (e: Exception) {
                        stopLoadingAnimation() // Stop if launch fails
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}", e)
                        Toast.makeText(this, "Google Sign-In not available.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener(this) { e ->
                    stopLoadingAnimation() // Stop on failure to begin sign-in
                    Log.e(TAG, "Google Sign-In begin failed: ${e.localizedMessage}", e)
                    traditionalGoogleSignIn()
                }
        }

        editPassword.setOnFocusChangeListener { _, hasFocus ->
            iconTogglePassword.visibility = if (hasFocus) ImageView.VISIBLE else ImageView.INVISIBLE
        }

        editPassword.setOnClickListener {
            iconTogglePassword.visibility = ImageView.VISIBLE
        }

        iconTogglePassword.setOnClickListener{
            togglePasswordVisibility()
        }

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
                startLoadingAnimation(listOf(loginButton, googleSignInButton, registerButton)) // Start loading
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        stopLoadingAnimation() // Stop loading
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                                    .putBoolean("logged_in", true)
                                    .putString("loggedInUserEmail", email)
                                    .putString("loggedInUserId", userId) // Save userId
                                    .apply()
                            }
                            navigateToMainActivity()
                        } else {
                            val error = task.exception?.message ?: "Authentication failed"
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show() // Show toast for login errors
                            emailInput.error = " " // Set a non-empty error to show the icon, actual message in Toast
                            passwordInput.error = " "
                        }
                    }
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Helper function to start loading animation
    private fun startLoadingAnimation(buttonsToDisable: List<Button>) {
        buttonsToDisable.forEach { button ->
            button.isEnabled = false
            button.alpha = 0.5f // Visually indicate disabled state
        }
        emailInput.isEnabled = false // Disable text inputs too
        passwordInput.isEnabled = false
        lottieLoadingView.visibility = View.VISIBLE
        lottieLoadingView.playAnimation()
    }

    // Helper function to stop loading animation
    private fun stopLoadingAnimation(buttonsToEnable: List<Button>? = null) {
        val buttons = buttonsToEnable ?: listOf(loginButton, googleSignInButton, registerButton)
        buttons.forEach { button ->
            button.isEnabled = true
            button.alpha = 1.0f
        }
        emailInput.isEnabled = true
        passwordInput.isEnabled = true
        lottieLoadingView.cancelAnimation()
        lottieLoadingView.visibility = View.GONE
    }


    // Fallback for traditional Google Sign-In if One Tap fails
    private fun traditionalGoogleSignIn() {
        Log.d(TAG, "Falling back to traditional Google Sign-In flow.")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Crucial for Firebase
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        // Before launching, sign out any existing traditional Google user to ensure account chooser
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncherForTraditional.launch(signInIntent) // Use a separate launcher
        }
    }
    // Separate launcher for traditional Google Sign-In result
    private val googleSignInLauncherForTraditional = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // stopLoadingAnimation() // Stop animation when traditional flow returns
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account?.idToken != null) {
                    // Start loading again specifically for Firebase auth part
                    startLoadingAnimation(listOf(googleSignInButton, loginButton, registerButton))
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    stopLoadingAnimation() // Make sure to stop if there's no token
                    Log.e(TAG, "Traditional Google Sign-In: ID token is null.")
                    Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                stopLoadingAnimation() // Stop on exception
                Log.e(TAG, "Traditional Google Sign-In failed: ${e.statusCode}", e)
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            stopLoadingAnimation() // Stop if cancelled
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase auth with Google successful.")
                    task.result.user?.let { user ->
                        handleSuccessfulLogin(user, "Google")
                    }
                } else {
                    stopLoadingAnimation()
                    Log.e(TAG, "Firebase auth with Google failed.", task.exception)
                    Toast.makeText(this, "Google authentication failed with Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleSuccessfulLogin(firebaseUser: FirebaseUser, providerName: String) {
        Log.d(TAG, "User logged in: ${firebaseUser.uid}, Provider: $providerName")

        val email = firebaseUser.email ?: "Not provided by $providerName"
        val displayName = firebaseUser.displayName ?: "User"
        val providerDataMap = firebaseUser.providerData.associate { it.providerId to it.email }


        val userData = mutableMapOf<String, Any>(
            "email" to (firebaseUser.email ?: "default_email@example.com"), // Provide a default if null
            "displayName" to (firebaseUser.displayName ?: "User"),
            "provider" to providerName,
            "lastLogin" to System.currentTimeMillis()
        )
        // Add provider specific emails if available
        firebaseUser.providerData.forEach { providerInfo ->
            providerInfo.email?.let { provEmail ->
                userData["${providerInfo.providerId}_email"] = provEmail
            }
        }

        saveUserToFirestore(firebaseUser, userData, providerName)
    }

    private fun saveUserToFirestore(firebaseUser: FirebaseUser, additionalData: Map<String, Any>, providerName: String) {
        val userRef = firestore.collection("users").document(firebaseUser.uid)

        val finalUserData = additionalData.toMutableMap()
        // Ensure email is present, even if it's a placeholder
        if (finalUserData["email"] == null || finalUserData["email"].toString().isBlank()) {
            finalUserData["email"] = "user_${firebaseUser.uid}@example.com" // Placeholder email
        }

        userRef.set(finalUserData, SetOptions.merge()) // Use merge to avoid overwriting existing non-auth fields
            .addOnSuccessListener {
                Log.d(TAG, "$providerName user data saved/merged in Firestore.")
                stopLoadingAnimation()
                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                    .putBoolean("logged_in", true)
                    .putString("loggedInUserEmail", finalUserData["email"].toString())
                    .putString("loggedInUserId", firebaseUser.uid)
                    .apply()

                lifecycleScope.launch {
                    PreloadedCategories.preloadUserCategories(firebaseUser.uid)
                    navigateToMainActivity()
                }
            }
            .addOnFailureListener { e ->
                stopLoadingAnimation() // Stop animation on failure
                Log.e(TAG, "Error saving/merging $providerName user data to Firestore", e)
                Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                // Proceeding even if Firestore fails, but animation should stop.
                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                    .putBoolean("logged_in", true)
                    .putString("loggedInUserEmail", finalUserData["email"].toString())
                    .putString("loggedInUserId", firebaseUser.uid)
                    .apply()
                navigateToMainActivity()
            }
    }

    //password viewer
    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            editPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            iconTogglePassword.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye))
        } else {
            // Show password
            editPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            iconTogglePassword.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_eye_off
                )

            )
        }
        // Move cursor to the end
        editPassword.setSelection(editPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this@ActivityLoginResgister, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}