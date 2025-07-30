package com.tk.pennywise

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var editCurrentPassword: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var textMessage: TextView

    private lateinit var editFirstName: EditText
    private lateinit var editSurname: EditText

    // Button and ProgressBar references
    private lateinit var buttonUpdate: Button
    private lateinit var progressBarUpdate: ProgressBar
    private lateinit var buttonDeleteAccount: Button
    private lateinit var progressBarDelete: ProgressBar

    // To store original button texts
    private var originalUpdateText: CharSequence? = null
    private var originalDeleteText: CharSequence? = null


    private var isPasswordVisible = false

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUser: FirebaseUser? = null

    private val dataToUpdateInFirestore = hashMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        currentUser = auth.currentUser

        enableEdgeToEdge()
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
        editFirstName = findViewById(R.id.editFirstName)
        editSurname = findViewById(R.id.editSurname)

        // Init Buttons and ProgressBars
        buttonUpdate = findViewById(R.id.buttonUpdate)
        progressBarUpdate = findViewById(R.id.progressBarUpdate)
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount)
        progressBarDelete = findViewById(R.id.progressBarDelete)

        // Store original button text
        originalUpdateText = buttonUpdate.text
        originalDeleteText = buttonDeleteAccount.text


        // Setup listeners
        iconTogglePassword.setOnClickListener { togglePasswordVisibility() }
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        // Listeners for FrameLayouts or Buttons directly
        buttonUpdate.setOnClickListener { updateProfile() }
        buttonDeleteAccount.setOnClickListener { deleteAccount() }


        loadUserData()
    }
    // --- Helper functions for button progress state ---
    private fun showProgress(button: Button, progressBar: ProgressBar, originalText: CharSequence?) {
        button.text = "" // Hide text
        button.isEnabled = false
        progressBar.visibility = View.VISIBLE
        // Optionally disable other inputs here if needed
        setInputsEnabled(false)
    }

    private fun hideProgress(button: Button, progressBar: ProgressBar, originalText: CharSequence?) {
        button.text = originalText // Restore text
        button.isEnabled = true
        progressBar.visibility = View.GONE
        // Re-enable inputs
        setInputsEnabled(true)
    }

    private fun setInputsEnabled(enabled: Boolean) {
        editFirstName.isEnabled = enabled
        editSurname.isEnabled = enabled
        editEmail.isEnabled = enabled
        editPassword.isEnabled = enabled
        editCurrentPassword.isEnabled = enabled
        // You might want to conditionally re-enable buttonUpdate and buttonDeleteAccount
        // based on other logic, but this is a general approach.
        if (enabled) { // Only enable buttons if progress is hidden
            if (progressBarUpdate.visibility == View.GONE) buttonUpdate.isEnabled = true
            if (progressBarDelete.visibility == View.GONE) buttonDeleteAccount.isEnabled = true
        } else {
            buttonUpdate.isEnabled = false
            buttonDeleteAccount.isEnabled = false
        }
    }


    private fun loadUserData() {
        showProgress(buttonUpdate, progressBarUpdate, originalUpdateText) // Show progress during load
        val userToLoad = currentUser
        if (userToLoad == null) {
            Log.w("ProfileActivity", "loadUserData: No current user found.")
            showMessage("User not logged in. Please log in again.", R.color.red)
            setInputsEnabled(false) // Disable all inputs
            buttonUpdate.isEnabled = false // Explicitly disable update button
            buttonDeleteAccount.isEnabled = false // Disable delete button
            hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText) // Hide progress
            return
        }

        // Load email from Auth
        editEmail.setText(userToLoad.email)

        // Load name and surname from Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(userToLoad.uid).get().await()
                if (userDoc.exists()) {
                    val userProfile = userDoc.toObject(User::class.java)
                    withContext(Dispatchers.Main) {
                        editFirstName.setText(userProfile?.name ?: "")
                        editSurname.setText(userProfile?.surname ?: "")
                        Log.d("ProfileActivity", "Loaded name: ${userProfile?.name}, surname: ${userProfile?.surname}")
                    }
                } else {
                    Log.d("ProfileActivity", "No user profile document found in Firestore for ${userToLoad.uid}. Fields will be empty.")
                    withContext(Dispatchers.Main) {
                        // Fields are already empty by default, or you can explicitly clear them
                        editFirstName.setText("")
                        editSurname.setText("")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading user data from Firestore: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showMessage("Error loading profile data.", R.color.red)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    hideProgress(
                        buttonUpdate,
                        progressBarUpdate,
                        originalUpdateText
                    ) // Hide progress after load
                    setInputsEnabled(true) // Re-enable inputs after loading
                }
            }
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
        showProgress(buttonUpdate, progressBarUpdate, originalUpdateText) // Show progress
        val userToUpdate = currentUser
        if (userToUpdate == null) {
            showMessage("No user logged in. Cannot update.", R.color.red)
            hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText) // Hide progress
            return
        }

        val newFirstName = editFirstName.text.toString().trim()
        val newSurname = editSurname.text.toString().trim()
        val newEmail = editEmail.text.toString().trim()
        val newPassword = editPassword.text.toString().trim()
        val currentPassword = editCurrentPassword.text.toString().trim()

        var isValid = true
        if (newFirstName.isEmpty()) {
            editFirstName.error = "First name cannot be empty"
            // return // Optional: allow updating other fields even if name is empty, depending on requirements
        }
        if (newSurname.isEmpty()) {
            editSurname.error = "Surname cannot be empty"
            // return // Optional
        }

        val isUpdatingAuthDetails = newEmail != userToUpdate.email || newPassword.isNotEmpty()

        if (isUpdatingAuthDetails) {
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                editEmail.error = "Enter a valid email"
                return
            }
            if (newPassword.isNotEmpty() && newPassword.length < 6) {
                editPassword.error = "New password must be at least 6 characters"
                return
            }
            if (currentPassword.isEmpty()) {
                editCurrentPassword.error = "Enter current password to update email/password"
                return
            }
        }
        if (!isValid) {
            hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText) // Hide progress on validation failure
            return
        }

        dataToUpdateInFirestore.clear() // Clear previous data
        if (newFirstName.isNotBlank()) dataToUpdateInFirestore["name"] = newFirstName
        if (newSurname.isNotBlank()) dataToUpdateInFirestore["surname"] = newSurname

        // --- Firestore Update for Name/Surname (can happen without re-auth) ---
        if (dataToUpdateInFirestore.isNotEmpty()) {
            firestore.collection("users").document(userToUpdate.uid)
                .set(dataToUpdateInFirestore, SetOptions.merge()) // Use merge to only update specified fields
                .addOnSuccessListener {
                    Log.d("ProfileActivity", "Name/Surname updated in Firestore.")
                    updateSharedPrefsNameSurname(newFirstName, newSurname)
                    if (!isUpdatingAuthDetails) {
                        showMessage("Name/Surname updated successfully!", R.color.teal_700)
                        hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText)
                    } else {
                        performAuthDetailsUpdate(userToUpdate, newEmail, newPassword, currentPassword, newFirstName, newSurname)
                    }
                }
                .addOnFailureListener { e ->
                    showMessage("Failed to update name/surname: ${e.message}", R.color.red)
                    Log.e("ProfileActivity", "Firestore name/surname update failed", e)
                    hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText)
                }
        } else if (isUpdatingAuthDetails) {
            // Only Auth details are being updated, no name/surname changes
            performAuthDetailsUpdate(userToUpdate, newEmail, newPassword, currentPassword, newFirstName, newSurname)
        }  else {
            showMessage("No changes to update.", R.color.orange)
            hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText)
        }
    }

    private fun performAuthDetailsUpdate(
        userToUpdate: FirebaseUser,
        newEmail: String,
        newPasswordString: String,
        currentPasswordString: String,
        updatedFirstName: String, // Pass along to update SharedPreferences together
        updatedSurname: String   // Pass along
    ) {
        val credential = EmailAuthProvider.getCredential(userToUpdate.email!!, currentPasswordString)
        userToUpdate.reauthenticate(credential)
            .addOnSuccessListener {
            Log.d("ProfileActivity", "User re-authenticated successfully.")
            val authTasks = mutableListOf<Task<Void>>()

            if (newEmail != userToUpdate.email) {
                authTasks.add(userToUpdate.updateEmail(newEmail))
            }
            if (newPasswordString.isNotEmpty()) {
                authTasks.add(userToUpdate.updatePassword(newPasswordString))
            }

            if (authTasks.isEmpty() && !dataToUpdateInFirestore.isNotEmpty()) { // Check if name/surname also changed
                showMessage("No changes to update for email/password.", R.color.orange)
                return@addOnSuccessListener
            }

                Tasks.whenAllComplete(authTasks)
                    .addOnCompleteListener { taskResult ->
                        var allAuthUpdatesSuccessful = true
                        val errorMessages = mutableListOf<String>()
                        taskResult.result.forEach { individualTask ->
                            if (!individualTask.isSuccessful) {
                                allAuthUpdatesSuccessful = false
                                errorMessages.add(individualTask.exception?.message ?: "Unknown error")
                            }
                        }
                        if (allAuthUpdatesSuccessful) {
                            if (newEmail != userToUpdate.email) updateFirestoreUserEmail(userToUpdate.uid, newEmail)
                            updateSharedPrefs(newEmail, updatedFirstName, updatedSurname)
                            showMessage("Profile updated successfully!", R.color.main_purple)
                            editPassword.setText("")
                            editCurrentPassword.setText("")
                        } else {
                            showMessage("Failed to update email/password: ${errorMessages.joinToString("; ")}", R.color.red)
                        }
                        hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText)
                    }
            }
            .addOnFailureListener { e ->
                showMessage("Re-authentication failed: ${e.message}", R.color.red)
                hideProgress(buttonUpdate, progressBarUpdate, originalUpdateText)
            }
    }

    private fun deleteAccount() {
        textMessage.visibility = TextView.GONE
        val userToDelete = currentUser
        if (userToDelete == null) {
            showMessage("No user logged in.", R.color.red)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                showProgress(buttonDeleteAccount, progressBarDelete, originalDeleteText) // Show progress for delete
                proceedWithDeletion(userToDelete)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun proceedWithDeletion(userToDelete: FirebaseUser) {
        val userId = userToDelete.uid
        Log.d("ProfileActivity", "Starting account deletion for user: $userId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Delete transactions subcollection
                val transactionsRef = firestore.collection("users").document(userId).collection("transactions")
                deleteCollection(transactionsRef)
                Log.d("ProfileActivity", "Transactions deleted for $userId")

                // 2. Delete main user document in "users" collection
                firestore.collection("users").document(userId).delete().await()
                Log.d("ProfileActivity", "User document deleted from Firestore for $userId")

                val rootCollectionsWithUserId = listOf(
                    "categories", "earnedBadges", "loginStreaks",
                    "budgetGoals", "categoryLimits", "feedback"
                )
                rootCollectionsWithUserId.forEach { collectionName ->
                    val querySnapshot = firestore.collection(collectionName)
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                    Log.d("ProfileActivity", "Data deleted from $collectionName for $userId")
                }

                // 4. Finally delete Firebase Auth account
                userToDelete.delete().await()
                Log.d("ProfileActivity", "Firebase Auth user deleted: $userId")

                withContext(Dispatchers.Main) {
                    clearLocalPrefs() // Clear SharedPreferences
                    showMessage("Account deleted successfully.", R.color.teal_700)
                    hideProgress(buttonDeleteAccount, progressBarDelete, originalDeleteText) // Hide progress
                    textMessage.postDelayed({
                        startActivity(Intent(this@ProfileActivity, RegisterActivity::class.java).apply {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish() // Finish ProfileActivity
                    }, 1500)
                }

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error during account deletion: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showMessage("Error deleting account: ${e.message}. Please try again.", R.color.red)
                    hideProgress(buttonDeleteAccount, progressBarDelete, originalDeleteText) // Hide progress
                }
            }
        }
    }

    // Helper to delete all documents in a collection/subcollection
    private suspend fun deleteCollection(collectionRef: CollectionReference, batchSize: Int = 100) {
        var query = collectionRef.orderBy(FieldPath.documentId()).limit(batchSize.toLong())
        while (true) {
            val snapshot = query.get().await()
            if (snapshot.isEmpty) break

            val batch = collectionRef.firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            val lastVisible = snapshot.documents.lastOrNull() ?: break
            query = collectionRef.orderBy(FieldPath.documentId()).startAfter(lastVisible).limit(batchSize.toLong())
        }
    }

    private fun updateFirestoreUserEmail(uid: String, newEmail: String) {
        firestore.collection("users").document(uid)
            .update("email", newEmail)
            .addOnSuccessListener { Log.d("ProfileActivity", "Firestore email updated for user $uid") }
            .addOnFailureListener { e -> Log.e("ProfileActivity", "Error updating Firestore email for $uid", e) }
    }

    private fun updateSharedPrefs(newEmail: String, newFirstName: String?, newSurname: String?) {
        getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit().apply {
            putString("loggedInUserEmail", newEmail)
            if (!newFirstName.isNullOrEmpty()) putString("userName", newFirstName) else remove("userName")
            if (!newSurname.isNullOrEmpty()) putString("userSurname", newSurname) else remove("userSurname")
            apply()
        }
        Log.d("ProfileActivity", "SharedPreferences updated with email: $newEmail, name: $newFirstName, surname: $newSurname")
    }

    // Specific SharedPreferences update for only name/surname
    private fun updateSharedPrefsNameSurname(newFirstName: String?, newSurname: String?) {
        getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit().apply {
            if (!newFirstName.isNullOrEmpty()) putString("userName", newFirstName) else remove("userName")
            if (!newSurname.isNullOrEmpty()) putString("userSurname", newSurname) else remove("userSurname")
            apply()
        }
        Log.d("ProfileActivity", "SharedPreferences updated with name: $newFirstName, surname: $newSurname")
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

    private fun clearLocalPrefs() { // Renamed to avoid confusion with server-side data
        getSharedPreferences("PennyWisePrefs", MODE_PRIVATE).edit().apply {
            remove("loggedInUserEmail")
            // remove("loggedInUserPassword") // Storing raw passwords in prefs is not secure
            remove("loggedInUserId") // Switched to loggedInUserId previously
            remove("userName")
            remove("userSurname")
            // remove("loggedInUser") // This seems to be a generic key, better to be specific
            // remove("loggedInUserUid") // Duplicates loggedInUserId
            // remove("loggedInUserPhotoUrl")
            putBoolean("logged_in", false) // If you use this flag
            apply()
        }
        Log.d("ProfileActivity", "Local SharedPreferences cleared.")
    }

    private fun showMessage(msg: String, colorResId: Int) {
        textMessage.apply {
            text = msg
            setTextColor(ContextCompat.getColor(context, colorResId))
            visibility = TextView.VISIBLE
        }
    }
}
