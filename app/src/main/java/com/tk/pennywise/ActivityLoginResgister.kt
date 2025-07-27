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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text

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
    private lateinit var googleSignInLauncherForTraditional: ActivityResultLauncher<Intent>


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

        setupGoogleSignIn()
        setupTraditionalGoogleSignInLauncher()

        googleSignInButton.setOnClickListener {
            Log.d(TAG, "Google Sign-In button clicked.")
            // Start loading animation immediately when Google button is clicked
            startLoadingAnimation(listOf(googleSignInButton, loginButton, registerButton))
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        Log.d(TAG, "One-Tap beginSignIn success. Launching UI.")
                        googleSignInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                        // Animation continues, will be stopped by launcher result or firebaseAuthWithGoogle
                    } catch (e: Exception) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}", e)
                        Toast.makeText(this, "Google Sign-In not available.", Toast.LENGTH_SHORT).show()
                        stopLoadingAnimation() // Stop animation if launch fails
                    }
                }
                .addOnFailureListener(this) { e ->
                    Log.w(TAG, "Google One Tap begin failed: ${e.localizedMessage}", e)
                    // Fallback to traditional sign-in, animation is already started
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
                startLoadingAnimation(listOf(loginButton, googleSignInButton, registerButton))
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Call handleSuccessfulLogin for email/password as well
                            task.result.user?.let { user ->
                                handleSuccessfulLogin(user, "Email") // "Email" as providerName
                            } ?: run {
                                // Should not happen if task.isSuccessful is true and user is not null
                                Log.e(TAG, "Email/Pass login successful but user is null.")
                                Toast.makeText(this, "Login error: User not found after sign-in.", Toast.LENGTH_LONG).show()
                                stopLoadingAnimation()
                            }
                        } else {
                            stopLoadingAnimation()
                            val error = task.exception?.message ?: "Authentication failed"
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                            emailInput.error = " "
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
        if (lottieLoadingView.visibility == View.VISIBLE) return // Already loading

        Log.d(TAG, "Starting loading animation.")
        buttonsToDisable.forEach { button ->
            button.isEnabled = false
            button.alpha = 0.5f
        }
        emailInput.isEnabled = false
        passwordInput.isEnabled = false
        lottieLoadingView.visibility = View.VISIBLE
        lottieLoadingView.playAnimation()
    }

    // Helper function to stop loading animation
    private fun stopLoadingAnimation() {
        Log.d(TAG, "Stopping loading animation.")
        val buttonsToEnable = listOf(loginButton, googleSignInButton, registerButton)
        buttonsToEnable.forEach { button ->
            button.isEnabled = true
            button.alpha = 1.0f
        }
        emailInput.isEnabled = true
        passwordInput.isEnabled = true
        if (lottieLoadingView.isAnimating) {
            lottieLoadingView.cancelAnimation()
        }
        lottieLoadingView.visibility = View.GONE
    }

    private fun setupGoogleSignIn() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false) // Set to false to always show account chooser, true for auto attempt
            .build()

        googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                Log.d(TAG, "Google One Tap RESULT: resultCode = ${result.resultCode}") // ADD THIS
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Google One Tap: RESULT_OK") // ADD THIS
                    try {
                        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                        val idToken = credential.googleIdToken
                        Log.d(TAG, "Google One Tap: idToken is ${if (idToken == null) "NULL" else "PRESENT"}") // ADD THIS
                        if (idToken != null) {
                            Log.d(TAG, "Google One Tap: Calling firebaseAuthWithGoogle with token.") // ADD THIS
                            firebaseAuthWithGoogle(idToken)
                        } else {
                            Log.e(TAG, "Google One Tap: No ID token!")
                            Toast.makeText(this, "Google Sign-In failed (no token).", Toast.LENGTH_SHORT).show()
                            stopLoadingAnimation()
                        }
                    } catch (e: ApiException) {
                        Log.e(TAG, "Google One Tap Sign-In failed in try-catch: ${e.statusCode}", e) // IMPROVE LOGGING
                        Toast.makeText(this, "Google Sign-In error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        stopLoadingAnimation()
                    }
                } else {
                    Log.d(TAG, "Google One Tap cancelled or failed. Result code: ${result.resultCode}")
                    stopLoadingAnimation()
                }
            }
    }

    private fun setupTraditionalGoogleSignInLauncher() {
        googleSignInLauncherForTraditional = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Traditional Google Sign-In RESULT: resultCode = ${result.resultCode}") // ADD THIS
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Traditional Google Sign-In: RESULT_OK") // ADD THIS
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "Traditional Google Sign-In: account is ${if (account == null) "NULL" else "PRESENT"}") // ADD THIS
                    Log.d(TAG, "Traditional Google Sign-In: account.idToken is ${if (account?.idToken == null) "NULL" else "PRESENT"}") // ADD THIS
                    if (account?.idToken != null) {
                        Log.d(TAG, "Traditional Google Sign-In: Calling firebaseAuthWithGoogle with token.") // ADD THIS
                        firebaseAuthWithGoogle(account.idToken!!)
                    } else {
                        stopLoadingAnimation()
                        Log.e(TAG, "Traditional Google Sign-In: ID token is null.")
                        Toast.makeText(this, "Google Sign-In failed (no token).", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    stopLoadingAnimation()
                    Log.e(TAG, "Traditional Google Sign-In failed in try-catch: ${e.statusCode}", e) // IMPROVE LOGGING
                    Toast.makeText(this, "Google Sign-In error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d(TAG, "Traditional Google Sign-In cancelled or failed. Result: ${result.resultCode}")
                stopLoadingAnimation()
            }
        }
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "firebaseAuthWithGoogle called with token: $idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                Log.d(TAG, "firebaseAuthWithGoogle: Firebase signInWithCredential task successful: ${task.isSuccessful}") // ADD THIS
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase auth with Google successful.")
                    task.result.user?.let { user ->
                        handleSuccessfulLogin(user, "Google")
                        // handleSuccessfulLogin will call stopLoadingAnimation after Firestore ops
                    } ?: run {
                        Log.e(TAG, "Firebase auth successful but user is null.")
                        Toast.makeText(this, "Authentication error.", Toast.LENGTH_SHORT).show()
                        stopLoadingAnimation()
                    }
                } else {
                    stopLoadingAnimation()
                    Log.e(TAG, "Firebase auth with Google failed.", task.exception)
                    Toast.makeText(this, "Google authentication failed with Firebase: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleSuccessfulLogin(firebaseUser: FirebaseUser, providerName: String) {
        Log.d(TAG, "Handling successful login for: ${firebaseUser.uid}, Provider: $providerName")

        val userRef = firestore.collection("users").document(firebaseUser.uid)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val existingData = documentSnapshot.data
                val finalUserData = mutableMapOf<String, Any>()

                // Essential data
                finalUserData["email"] = firebaseUser.email ?: "user_${firebaseUser.uid}@example.com"
                finalUserData["provider"] = providerName
                finalUserData["lastLogin"] = System.currentTimeMillis()
                firebaseUser.displayName?.let { finalUserData["displayName"] = it } // Store original display name

                var nameToSave: String? = existingData?.get("name") as? String
                var surnameToSave: String? = existingData?.get("surname") as? String

                if (nameToSave.isNullOrEmpty() && surnameToSave.isNullOrEmpty() && providerName == "Google" && !firebaseUser.displayName.isNullOrEmpty()) {
                    // First time Google login and no name/surname in Firestore, try to parse displayName
                    val displayNameParts = firebaseUser.displayName!!.split(" ")
                    nameToSave = displayNameParts.firstOrNull()
                    if (displayNameParts.size > 1) {
                        surnameToSave = displayNameParts.drop(1).joinToString(" ")
                    }
                    nameToSave?.let { finalUserData["name"] = it }
                    surnameToSave?.let { finalUserData["surname"] = it }
                } else {
                    // Use existing Firestore name/surname if available
                    nameToSave?.let { finalUserData["name"] = it }
                    surnameToSave?.let { finalUserData["surname"] = it }
                }

                // Add provider specific emails if available (optional, but good for diagnostics)
                firebaseUser.providerData.forEach { providerInfo ->
                    providerInfo.email?.let { provEmail ->
                        finalUserData["${providerInfo.providerId}_email"] = provEmail
                    }
                }

                userRef.set(finalUserData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "$providerName user data saved/merged in Firestore.")
                        saveToSharedPreferences(
                            firebaseUser.uid,
                            finalUserData["email"].toString(),
                            nameToSave, // Use the determined nameToSave
                            surnameToSave // Use the determined surnameToSave
                        )
                        lifecycleScope.launch {
                            PreloadedCategories.preloadUserCategories(firebaseUser.uid) // Keep if this is used
                            navigateToMainActivity() // This will also stop animation if called from a path that needs it
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving/merging $providerName user data to Firestore", e)
                        Toast.makeText(this, "Failed to save user profile.", Toast.LENGTH_SHORT).show()
                        // Fallback: Still save basic login info to prefs and proceed
                        saveToSharedPreferences(
                            firebaseUser.uid,
                            finalUserData["email"].toString(),
                            null, // No name/surname if Firestore save failed before they were confirmed
                            null
                        )
                        navigateToMainActivity()
                    }
            }
            .addOnFailureListener { e ->
                // This means we couldn't even GET the user document
                Log.e(TAG, "Error fetching user document from Firestore for merging", e)
                Toast.makeText(this, "Could not load user profile. Logging in with basic info.", Toast.LENGTH_LONG).show()

                // If fetching fails, we can't know existing name/surname.
                // For Google, we can still try to parse displayName if it's a new login.
                val tempUserData = mutableMapOf<String, Any>(
                    "email" to (firebaseUser.email ?: "user_${firebaseUser.uid}@example.com"),
                    "provider" to providerName,
                    "lastLogin" to System.currentTimeMillis()
                )
                var tempNameToSave: String? = null
                var tempSurnameToSave: String? = null

                if (providerName == "Google" && !firebaseUser.displayName.isNullOrEmpty()) {
                    val displayNameParts = firebaseUser.displayName!!.split(" ")
                    tempNameToSave = displayNameParts.firstOrNull()
                    if (displayNameParts.size > 1) {
                        tempSurnameToSave = displayNameParts.drop(1).joinToString(" ")
                    }
                    tempNameToSave?.let { tempUserData["name"] = it }
                    tempSurnameToSave?.let { tempUserData["surname"] = it }
                }
                // Attempt to save this basic data or data from Google displayName
                firestore.collection("users").document(firebaseUser.uid)
                    .set(tempUserData, SetOptions.merge()) // Try to save what we have
                    .addOnCompleteListener { saveTask -> // Using onCompleteListener to handle both success/failure of this save
                        if (!saveTask.isSuccessful) {
                            Log.w(TAG, "Fallback save to Firestore also failed.", saveTask.exception)
                        }
                        saveToSharedPreferences(
                            firebaseUser.uid,
                            tempUserData["email"].toString(),
                            tempNameToSave,
                            tempSurnameToSave
                        )
                        navigateToMainActivity()
                    }
            }
    }

    private fun saveToSharedPreferences(userId: String, email: String, name: String?, surname: String?) {
        val prefs = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
        prefs.putBoolean("logged_in", true)
        prefs.putString("loggedInUserId", userId)
        prefs.putString("loggedInUserEmail", email)

        if (!name.isNullOrEmpty()) {
            prefs.putString("userName", name)
            Log.d(TAG, "Saved to SharedPreferences: Name='$name'")
        } else {
            prefs.remove("userName")
            Log.d(TAG, "Removed userName from SharedPreferences or was null")
        }
        if (!surname.isNullOrEmpty()) {
            prefs.putString("userSurname", surname)
            Log.d(TAG, "Saved to SharedPreferences: Surname='$surname'")
        } else {
            prefs.remove("userSurname")
            Log.d(TAG, "Removed userSurname from SharedPreferences or was null")
        }
        prefs.apply()
    }

    //password viewer
    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            iconTogglePassword.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye))
        } else {
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            iconTogglePassword.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye_off))
        }
        editPassword.setSelection(editPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun navigateToMainActivity() {
        stopLoadingAnimation() // Ensure animation stops before navigating
        startActivity(Intent(this@ActivityLoginResgister, MainActivity::class.java))
        finishAffinity() // Clear back stack
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}