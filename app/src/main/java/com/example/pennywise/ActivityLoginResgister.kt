package com.example.pennywise

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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.launch

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

        val googleSignInButton: Button = findViewById(R.id.buttonGoogleSignIn)

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
                if (result.resultCode == Activity.RESULT_OK) {
                    try {
                        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                        val idToken = credential.googleIdToken
                        when {
                            idToken != null -> {
                                firebaseAuthWithGoogle(idToken)
                            }
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
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        googleSignInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}", e)
                        Toast.makeText(this, "Google Sign-In not available.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener(this) { e ->
                    Log.e(TAG, "Google Sign-In begin failed: ${e.localizedMessage}", e)
                    // Fallback to traditional Google Sign-In if One Tap fails or is unavailable
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
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                                    .putBoolean("logged_in", true)
                                    .putString("loggedInUserEmail", email)
                                    .putString("loggedInUserId", userId) // Save userId
                                    .apply()
                            }

                            startActivity(Intent(this@ActivityLoginResgister, MainActivity::class.java))
                            finish()
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        } else {
                            val error = task.exception?.message ?: "Authentication failed"
                            emailInput.error = error
                            passwordInput.error = error
                        }
                    }
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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
    // Separate launcher for traditional Google Sign-In result
    private val googleSignInLauncherForTraditional = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account?.idToken != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    Log.e(TAG, "Traditional Google Sign-In: ID token is null.")
                    Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Traditional Google Sign-In failed: ${e.statusCode}", e)
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //callbackManager.onActivityResult(requestCode, resultCode, data) // For Facebook
        //super.onActivityResult(requestCode, resultCode, data)
        // Note: Modern ActivityResultLauncher is preferred over this,
        // but Facebook SDK might still rely on it.
    //}

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
                    Log.e(TAG, "Firebase auth with Google failed.", task.exception)
                    Toast.makeText(this, "Google authentication failed with Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun firebaseAuthWithPlayGames(idToken: String, displayName: String?, email: String?) {
        // Important: For Play Games, you use GoogleAuthProvider with the ID token obtained
        // via the GoogleSignInAccount linked to Play Games.
        // PlayGamesAuthProvider is generally used with serverAuthCode, which is a different flow.
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase auth with Play Games (via Google token) successful.")
                    task.result.user?.let { user ->
                        // Pass along displayName and email if you want to store them
                        // You might want to merge this with what Firebase already has for the user.
                        val providerData = user.providerData.find { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                        val actualEmail = providerData?.email ?: email ?: user.email
                        val actualDisplayName = providerData?.displayName ?: displayName ?: user.displayName

                        val userData = mutableMapOf<String, Any>()
                        if (actualEmail != null) userData["email"] = actualEmail
                        if (actualDisplayName != null) userData["displayName"] = actualDisplayName
                        userData["provider"] = "PlayGames" // Or "Google" if you treat them the same in Firestore

                        saveUserToFirestore(user, userData, "Play Games")
                    }
                } else {
                    Log.e(TAG, "Firebase auth with Play Games (via Google token) failed.", task.exception)
                    Toast.makeText(this, "Play Games authentication failed with Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun firebaseAuthWithFacebook(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase auth with Facebook successful.")
                    task.result.user?.let { user ->
                        handleSuccessfulLogin(user, "Facebook")
                    }
                } else {
                    Log.e(TAG, "Firebase auth with Facebook failed.", task.exception)
                    Toast.makeText(this, "Facebook authentication failed with Firebase.", Toast.LENGTH_SHORT).show()
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
                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                    .putBoolean("logged_in", true)
                    .putString("loggedInUserEmail", finalUserData["email"].toString())
                    .putString("loggedInUserId", firebaseUser.uid)
                    .apply()

                lifecycleScope.launch {
                    PreloadedCategories.preloadUserCategories(firebaseUser.uid) // Ensure this is present or remove
                    startActivity(Intent(this@ActivityLoginResgister, MainActivity::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving/merging $providerName user data to Firestore", e)
                Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                // Still proceed to main activity if auth was successful but Firestore failed,
                // or handle this more gracefully (e.g., sign out user).
                // For now, let's proceed to allow user in if Firebase Auth succeeded.
                getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
                    .putBoolean("logged_in", true)
                    .putString("loggedInUserEmail", finalUserData["email"].toString())
                    .putString("loggedInUserId", firebaseUser.uid)
                    .apply()
                startActivity(Intent(this@ActivityLoginResgister, MainActivity::class.java))
                finish()
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
}