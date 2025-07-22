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
                if (result.resultCode == RESULT_OK) {
                    try {
                        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                        val idToken = credential.googleIdToken
                        when {
                            idToken != null -> {
                                firebaseAuthWithGoogle(idToken)
                            }
                            else -> {
                                Log.e(TAG, "Google Sign-In: No ID token!")
                                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                                stopLoadingAnimation() // Explicitly stop if we don't proceed
                            }
                        }
                    } catch (e: ApiException) {
                        Log.e(TAG, "Google Sign-In failed: ${e.statusCode}", e)
                        Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                        stopLoadingAnimation()
                    }
                } else {
                    Log.d(TAG, "Google Sign-In cancelled or failed. Result code: ${result.resultCode}")
                    Toast.makeText(this, "Google Sign-In cancelled.", Toast.LENGTH_SHORT).show()
                    stopLoadingAnimation()
                }
            }

        googleSignInButton.setOnClickListener {
            googleSignInButton.isEnabled = false
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        // Start loading animation immediately for Google button
                        startLoadingAnimation(listOf(googleSignInButton, loginButton, registerButton))
                        // The launcher will handle stopping the animation or continuing it if firebaseAuthWithGoogle starts it again
                        googleSignInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}", e)
                        Toast.makeText(this, "Google Sign-In not available.", Toast.LENGTH_SHORT).show()
                        stopLoadingAnimation() // Stop animation if launch fails
                        googleSignInButton.isEnabled = true // Re-enable
                    }
                }
                .addOnFailureListener(this) { e ->
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

        // Data that comes from the Auth provider (email, displayName potentially)
        val authProviderData = additionalData.toMutableMap()
        if (authProviderData["email"] == null || authProviderData["email"].toString().isBlank()) {
            authProviderData["email"] = "user_${firebaseUser.uid}@example.com"
        }

        userRef.get().addOnSuccessListener { documentSnapshot ->
            val existingData = documentSnapshot.data
            val finalUserData = authProviderData.toMutableMap()

            // Merge: Prioritize existing Firestore data for name/surname
            // Only use authProviderData's displayName if 'name' is not already in Firestore.
            if (existingData?.get("name") != null) {
                finalUserData["name"] = existingData["name"]!!
            } else if (authProviderData["displayName"] != null && authProviderData["displayName"].toString().isNotBlank() && finalUserData["name"] == null) {
                // If it's a first-time sign-in with Google and they have a display name,
                // you might want to pre-fill 'name'. Split it if possible.
                val displayNameParts = authProviderData["displayName"].toString().split(" ")
                finalUserData["name"] = displayNameParts.firstOrNull() ?: authProviderData["displayName"].toString()
                if (displayNameParts.size > 1) {
                    finalUserData["surname"] = displayNameParts.drop(1).joinToString(" ")
                }
            }

            if (existingData?.get("surname") != null) {
                finalUserData["surname"] = existingData["surname"]!!
            }

            // Always update lastLogin and provider
            finalUserData["lastLogin"] = System.currentTimeMillis()
            finalUserData["provider"] = providerName

            userRef.set(finalUserData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "$providerName user data saved/merged in Firestore.")
                    // NOW, save the fetched/merged name and surname to SharedPreferences
                    val prefs = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                    prefs.putBoolean("logged_in", true)
                    prefs.putString("loggedInUserEmail", finalUserData["email"].toString())
                    prefs.putString("loggedInUserId", firebaseUser.uid)

                    // Get name and surname from finalUserData to save in prefs
                    val nameToSave = finalUserData["name"]?.toString()
                    val surnameToSave = finalUserData["surname"]?.toString()

                    if (!nameToSave.isNullOrEmpty()) {
                        prefs.putString("userName", nameToSave)
                    } else {
                        prefs.remove("userName")
                    }
                    if (!surnameToSave.isNullOrEmpty()) {
                        prefs.putString("userSurname", surnameToSave)
                    } else {
                        prefs.remove("userSurname")
                    }
                    prefs.apply()

                    Log.d(TAG, "Saved to SharedPreferences: Name='${nameToSave}', Surname='${surnameToSave}'")


                    stopLoadingAnimation() // Stop animation here after all processing

                    lifecycleScope.launch {
                        PreloadedCategories.preloadUserCategories(firebaseUser.uid)
                        navigateToMainActivity()
                    }
                }
                .addOnFailureListener { e ->
                    stopLoadingAnimation()
                    Log.e(TAG, "Error saving/merging $providerName user data to Firestore", e)
                    Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                    // Fallback: Still save basic login info to prefs
                    getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                        .putBoolean("logged_in", true)
                        .putString("loggedInUserEmail", finalUserData["email"].toString()) // use email from finalUserData
                        .putString("loggedInUserId", firebaseUser.uid)
                        .apply()
                    navigateToMainActivity()
                }
        }.addOnFailureListener { e ->
            // Failed to fetch existing document, proceed with caution or handle error
            stopLoadingAnimation()
            Log.e(TAG, "Error fetching user document from Firestore before merge", e)
            Toast.makeText(this, "Could not load profile data. Please try again.", Toast.LENGTH_SHORT).show()
            // Potentially sign out the user or allow login with default/missing name/surname
            auth.signOut() // Example: force sign out if profile can't be loaded
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