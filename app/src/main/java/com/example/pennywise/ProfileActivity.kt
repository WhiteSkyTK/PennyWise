package com.example.pennywise


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.io.path.exists

class ProfileActivity : AppCompatActivity() {
    private lateinit var editCurrentPassword: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var iconTogglePassword: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var textMessage: TextView

    // New Views for Name and Surname
    private lateinit var editFirstName: EditText
    private lateinit var editSurname: EditText

    private var isPasswordVisible = false

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        currentUser = auth.currentUser

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

        // Init new views
        editFirstName = findViewById(R.id.editFirstName)
        editSurname = findViewById(R.id.editSurname)

        // Setup listeners
        iconTogglePassword.setOnClickListener { togglePasswordVisibility() }
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<Button>(R.id.buttonUpdate).setOnClickListener { updateProfile() }
        findViewById<Button>(R.id.buttonDeleteAccount).setOnClickListener { deleteAccount() }

        loadUserData()
    }

    private fun loadUserData() {
        val userToLoad = currentUser // Use the class member
        if (userToLoad == null) {
            Log.w("ProfileActivity", "loadUserData: No current user found.")
            // Optionally redirect to login or show an error
            showMessage("User not logged in. Please log in again.", R.color.red)
            // Disable input fields if no user
            editFirstName.isEnabled = false
            editSurname.isEnabled = false
            editEmail.isEnabled = false
            editPassword.isEnabled = false
            editCurrentPassword.isEnabled = false
            findViewById<Button>(R.id.buttonUpdate).isEnabled = false
            return
        }

        // Load email from Auth
        editEmail.setText(userToLoad.email)

        // Load name and surname from Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(userToLoad.uid).get().await()
                if (userDoc.exists()) {
                    val userProfile = userDoc.toObject(User::class.java) // Using your User data class
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
        val userToUpdate = currentUser // Use the class member

        if (userToUpdate == null) {
            showMessage("No user logged in. Cannot update.", R.color.red)
            return
        }

        val newFirstName = editFirstName.text.toString().trim()
        val newSurname = editSurname.text.toString().trim()
        val newEmail = editEmail.text.toString().trim()
        val newPassword = editPassword.text.toString().trim()
        val currentPassword = editCurrentPassword.text.toString().trim()

        if (newFirstName.isEmpty()) {
            editFirstName.error = "First name cannot be empty"
            // return // Optional: allow updating other fields even if name is empty, depending on requirements
        }
        if (newSurname.isEmpty()) {
            editSurname.error = "Surname cannot be empty"
            // return // Optional
        }


        // Email and Password Validations (keep as is if they are mandatory for any update)
        // If email/password updates are optional unless currentPassword is provided, adjust logic.
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


        // Proceed with updates
        val dataToUpdateInFirestore = hashMapOf<String, Any>()
        var nameChanged = false
        var surnameChanged = false

        // Check if name or surname actually changed to avoid unnecessary Firestore writes
        // and to give specific feedback. For this, we'd need to store the initial loaded values.
        // For simplicity now, we'll update if fields are not blank.
        if (newFirstName.isNotBlank()) {
            dataToUpdateInFirestore["name"] = newFirstName
            nameChanged = true // Assume changed if not blank for this example
        }
        if (newSurname.isNotBlank()) {
            dataToUpdateInFirestore["surname"] = newSurname
            surnameChanged = true // Assume changed
        }


        // --- Firestore Update for Name/Surname (can happen without re-auth) ---
        if (dataToUpdateInFirestore.isNotEmpty()) {
            firestore.collection("users").document(userToUpdate.uid)
                .set(dataToUpdateInFirestore, SetOptions.merge()) // Use merge to only update specified fields
                .addOnSuccessListener {
                    Log.d("ProfileActivity", "Name/Surname updated in Firestore.")
                    // Update SharedPreferences for name/surname
                    updateSharedPrefsNameSurname(newFirstName, newSurname)
                    if (!isUpdatingAuthDetails) { // If only name/surname changed
                        showMessage("Name/Surname updated successfully!", R.color.teal_700)
                    }
                    // Continue to Auth details update if needed
                    if (isUpdatingAuthDetails) {
                        performAuthDetailsUpdate(userToUpdate, newEmail, newPassword, currentPassword, newFirstName, newSurname)
                    }
                }
                .addOnFailureListener { e ->
                    showMessage("Failed to update name/surname: ${e.message}", R.color.red)
                    Log.e("ProfileActivity", "Firestore name/surname update failed", e)
                }
        } else if (isUpdatingAuthDetails) {
            // Only Auth details are being updated, no name/surname changes
            performAuthDetailsUpdate(userToUpdate, newEmail, newPassword, currentPassword, newFirstName, newSurname)
        } else {
            showMessage("No changes to update.", R.color.orange)
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

        userToUpdate.reauthenticate(credential).addOnSuccessListener {
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
                .addOnCompleteListener { taskResult -> // Use onCompleteListener for better error details
                    var allAuthUpdatesSuccessful = true
                    val errorMessages = mutableListOf<String>()

                    taskResult.result.forEach { individualTask ->
                        if (!individualTask.isSuccessful) {
                            allAuthUpdatesSuccessful = false
                            val exceptionMessage = individualTask.exception?.message ?: "Unknown error during auth update."
                            errorMessages.add(exceptionMessage)
                            Log.e("ProfileActivity", "Auth update task failed: $exceptionMessage", individualTask.exception)
                        }
                    }

                    if (allAuthUpdatesSuccessful) {
                        // Update Firestore email if it changed (name/surname already updated or handled)
                        if (newEmail != userToUpdate.email) { // Check if email was part of the update
                            updateFirestoreUserEmail(userToUpdate.uid, newEmail) // Update email in 'users' doc
                        }
                        // Update SharedPreferences (name, surname, email)
                        updateSharedPrefs(newEmail, updatedFirstName, updatedSurname)

                        showMessage("Profile updated successfully!", R.color.teal_700)
                        editPassword.setText("") // Clear new password field
                        editCurrentPassword.setText("") // Clear current password field
                    } else {
                        showMessage("Failed to update email/password: ${errorMessages.joinToString("; ")}", R.color.red)
                    }
                }
        }.addOnFailureListener { e ->
            showMessage("Re-authentication failed: ${e.message}", R.color.red)
            Log.e("ProfileActivity", "Re-authentication failed", e)
        }
    }

    private fun deleteAccount() {
        textMessage.visibility = TextView.GONE
        val userToDelete = currentUser // Use the class member

        if (userToDelete == null) {
            showMessage("No user logged in.", R.color.red)
            return
        }

        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                proceedWithDeletion(userToDelete)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun proceedWithDeletion(userToDelete: FirebaseUser) {
        val userId = userToDelete.uid
        Log.d("ProfileActivity", "Starting account deletion for user: $userId")

        // Display a progress message or spinner here if desired
        showMessage("Deleting account data...", R.color.orange) // Or use a ProgressBar

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Delete transactions subcollection
                val transactionsRef = firestore.collection("users").document(userId).collection("transactions")
                deleteCollection(transactionsRef)
                Log.d("ProfileActivity", "Transactions deleted for $userId")

                // Add other subcollections specific to your app if any under users/{userId}/...
                // Example: deleteCollection(firestore.collection("users").document(userId).collection("otherSubCollection"))

                // 2. Delete main user document in "users" collection
                firestore.collection("users").document(userId).delete().await()
                Log.d("ProfileActivity", "User document deleted from Firestore for $userId")

                // 3. Delete data from root collections where you store userId (if any)
                // This part depends heavily on your Firestore structure for things like 'categories', 'feedback', etc.
                // Assuming they are root collections and have a 'userId' field:
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
                    // Redirect to login/register screen
                    textMessage.postDelayed({
                        startActivity(Intent(this@ProfileActivity, RegisterActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish() // Finish ProfileActivity
                    }, 1500)
                }

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error during account deletion: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showMessage("Error deleting account: ${e.message}. Please try again.", R.color.red)
                }
            }
        }
    }

    // Helper to delete all documents in a collection/subcollection
    private suspend fun deleteCollection(collectionRef: com.google.firebase.firestore.CollectionReference, batchSize: Int = 100) {
        var query = collectionRef.orderBy(com.google.firebase.firestore.FieldPath.documentId()).limit(batchSize.toLong())
        while (true) {
            val snapshot = query.get().await()
            if (snapshot.isEmpty) break

            val batch = collectionRef.firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            val lastVisible = snapshot.documents.lastOrNull() ?: break
            query = collectionRef.orderBy(com.google.firebase.firestore.FieldPath.documentId()).startAfter(lastVisible).limit(batchSize.toLong())
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

    private val dataToUpdateInFirestore = hashMapOf<String, Any>()
}
