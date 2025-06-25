package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings

class ProfileActivity : AppCompatActivity() {
    private lateinit var editCurrentPassword: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var textMessage: TextView
    private var isPasswordVisible = false

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val user = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Enable Firestore offline persistence
        firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }

        // UI customization
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileLayout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Init views
        editCurrentPassword = findViewById(R.id.editCurrentPassword)
        editEmail = findViewById(R.id.editEmail)
        editPassword = findViewById(R.id.editPassword)
        iconTogglePassword = findViewById(R.id.iconTogglePassword)
        backButton = findViewById(R.id.backButton)
        textMessage = findViewById(R.id.textMessage)

        // Setup listeners
        iconTogglePassword.setOnClickListener { togglePasswordVisibility() }
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<Button>(R.id.buttonUpdate).setOnClickListener { updateProfile() }
        findViewById<Button>(R.id.buttonDeleteAccount).setOnClickListener { deleteAccount() }

        loadUserData()
    }

    private fun loadUserData() {
        user?.let {
            editEmail.setText(it.email)
        }
    }

    private fun togglePasswordVisibility() {
        val type = if (isPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        editPassword.inputType = type
        editPassword.setSelection(editPassword.text.length)

        val iconRes = if (isPasswordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
        iconTogglePassword.setImageDrawable(ContextCompat.getDrawable(this, iconRes))

        isPasswordVisible = !isPasswordVisible
    }

    private fun updateProfile() {
        textMessage.visibility = TextView.GONE

        val newEmail = editEmail.text.toString().trim()
        val newPassword = editPassword.text.toString().trim()
        val currentPassword = editCurrentPassword.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            editEmail.error = "Enter a valid email"
            return
        }

        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            editPassword.error = "Password must be at least 6 characters"
            return
        }

        if (currentPassword.isEmpty()) {
            editCurrentPassword.error = "Enter your current password to proceed"
            return
        }

        user?.let { currentUser ->
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)

            currentUser.reauthenticate(credential).addOnSuccessListener {
                val tasks = mutableListOf<com.google.android.gms.tasks.Task<Void>>()

                if (newEmail != currentUser.email) {
                    tasks.add(currentUser.updateEmail(newEmail))
                }

                if (newPassword.isNotEmpty()) {
                    tasks.add(currentUser.updatePassword(newPassword))
                }

                if (tasks.isEmpty()) {
                    showMessage("No changes to update.", R.color.orange)
                    return@addOnSuccessListener
                }

                // Chain update tasks
                com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener { results ->
                        var hasError = false
                        results.forEach {
                            if (!it.isSuccessful) {
                                hasError = true
                                showMessage("Update failed: ${it.exception?.message}", R.color.red)
                            }
                        }

                        if (!hasError) {
                            updateFirestoreEmail(newEmail)
                            updateSharedPrefs(newEmail)
                            showMessage("Profile updated successfully!", R.color.teal_700)
                            editPassword.setText("")
                        }
                    }
                    .addOnFailureListener {
                        showMessage("Update failed: ${it.message}", R.color.red)
                    }
            }.addOnFailureListener {
                showMessage("Re-authentication failed: ${it.message}", R.color.red)
            }
        } ?: showMessage("No user logged in.", R.color.red)
    }

    private fun deleteAccount() {
        textMessage.visibility = TextView.GONE

        user?.let { currentUser ->
            val userId = currentUser.uid

            // 1. Delete all user-related subcollections and documents
            val collectionsToDelete = listOf(
                "transactions",
                "categories",
                "earnedBadges",
                "loginStreaks",
                "budgetGoals",
                "categoryLimits",
                "feedback"
            )

            val deleteTasks = collectionsToDelete.map { collectionName ->
                firestore.collection(collectionName)
                    .whereEqualTo("userId", userId)
                    .get()
                    .continueWithTask { task ->
                        val batch = firestore.batch()
                        task.result?.forEach { doc ->
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                    }
            }.toMutableList()

            // 2. Delete main user document in "users" collection
            deleteTasks.add(
                firestore.collection("users").document(userId).delete()
            )

            // 3. Wait for all deletions to complete
            com.google.android.gms.tasks.Tasks.whenAllComplete(deleteTasks)
                .addOnSuccessListener {
                    // 4. Finally delete Firebase Auth account
                    currentUser.delete().addOnSuccessListener {
                        clearPrefs()
                        showMessage("Account deleted.", R.color.teal_700)

                        textMessage.postDelayed({
                            startActivity(Intent(this, RegisterActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }, 1000)
                    }.addOnFailureListener {
                        showMessage("Auth deletion failed: ${it.message}", R.color.red)
                    }
                }
                .addOnFailureListener {
                    showMessage("Failed to delete all user data: ${it.message}", R.color.red)
                }

        } ?: showMessage("No user logged in.", R.color.red)
    }

    private fun updateFirestoreEmail(newEmail: String) {
        user?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .update("email", newEmail)
        }
    }

    private fun updateSharedPrefs(newEmail: String) {
        getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
            .putString("loggedInUserEmail", newEmail)
            .apply()
    }

    private fun clearPrefs() {
        getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit()
            .remove("loggedInUserEmail")
            .remove("loggedInUserPassword")
            .remove("loggedInUser")
            .remove("loggedInUserUid")
            .remove("loggedInUserPhotoUrl")
            .putBoolean("logged_in", false)
            .apply()
    }

    private fun showMessage(msg: String, colorResId: Int) {
        textMessage.apply {
            text = msg
            setTextColor(ContextCompat.getColor(context, colorResId))
            visibility = TextView.VISIBLE
        }
    }
}
