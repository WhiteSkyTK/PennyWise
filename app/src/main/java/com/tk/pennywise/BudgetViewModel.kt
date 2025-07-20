package com.tk.pennywise


import android.app.Application
import android.util.Log
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isEmpty
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- isLoading LiveData ---
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    // -------------------------

    private val _monthlyGoal = MutableLiveData<BudgetGoal?>()
    val monthlyGoal: LiveData<BudgetGoal?> get() = _monthlyGoal
    private var monthlyGoalListener: ListenerRegistration? = null

    private val _categoryLimits = MutableLiveData<List<CategoryLimit>>()
    val categoryLimits: LiveData<List<CategoryLimit>> get() = _categoryLimits
    private var categoryLimitsListener: ListenerRegistration? = null

    fun loadMonthlyGoal(month: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _monthlyGoal.value = null
            Log.w("BudgetViewModel", "User not authenticated for loading monthly goal.")
            return
        }

        monthlyGoalListener?.remove() // Remove previous listener

        val goalQuery = firestore.collection("users").document(uid)
            .collection("budget_goals")
            .whereEqualTo("month", month)
        // .limit(1) // Assuming one goal per month

        monthlyGoalListener = goalQuery.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("BudgetViewModel", "Monthly goal listen failed for month: $month", e)
                _monthlyGoal.value = null
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                // Assuming BudgetGoal has an id field that can be set from document.id
                val goal = snapshots.documents.first().toObject(BudgetGoal::class.java)
                _monthlyGoal.value = goal
                Log.d("BudgetViewModel", "Monthly goal updated for $month: $goal")
            } else {
                _monthlyGoal.value = null
                Log.d("BudgetViewModel", "No monthly goal found for $month")
            }
        }
    }

    fun saveMonthlyGoal(goal: BudgetGoal) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("budget_goals")
            .document(goal.month) // Use month as document ID for simplicity if unique
            .set(goal)
            .addOnSuccessListener {
                Log.d("BudgetViewModel", "Monthly goal saved for ${goal.month}")
                // LiveData will be updated by the listener if loadMonthlyGoal was called for this month
            }
            .addOnFailureListener { e ->
                Log.e("BudgetViewModel", "Failed to save monthly goal", e)
            }
    }

    fun loadCategoryLimitsWithUsage(month: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _categoryLimits.value = emptyList()
            _isLoading.value = false // Not authenticated, stop loading indicator
            Log.w("BudgetViewModel", "User not authenticated for loading category limits.")
            return
        }

        Log.d("BudgetViewModel", "Setting up real-time listener for category limits for user: $uid and month: $month")

        categoryLimitsListener?.remove() // Remove previous listener

        val limitsQuery = firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("month", month)

        categoryLimitsListener = limitsQuery.addSnapshotListener { limitSnapshots, e ->
            if (e != null) {
                Log.e("BudgetViewModel", "Category limits listen failed for $month", e)
                _categoryLimits.value = emptyList()
                _isLoading.value = false // Error occurred, stop loading indicator
                return@addSnapshotListener
            }

            if (limitSnapshots == null || limitSnapshots.isEmpty) {
                Log.d("BudgetViewModel", "No category limits found in snapshot for $month.")
                _categoryLimits.value = emptyList()
                _isLoading.value = false // No data, stop loading indicator
                return@addSnapshotListener
            }

            Log.d("BudgetViewModel", "Fetched ${limitSnapshots.size()} category limit docs in real-time for $month")

            val limitsWithUsageTasks = mutableListOf<com.google.android.gms.tasks.Task<CategoryLimit>>()

            for (doc in limitSnapshots) {
                // Ensure your CategoryLimit data class has an 'id' field
                val limit = doc.toObject(CategoryLimit::class.java).copy(id = doc.id)
                Log.d("BudgetViewModel", "Processing limit: ${limit.category} (${limit.categoryId}) for $month")

                val task = firestore.collection("users").document(uid)
                    .collection("transactions")
                    .whereEqualTo("categoryId", limit.categoryId)
                    .whereEqualTo("monthYear", month) // Ensure your transactions have 'monthYear'
                    .get()
                    .continueWith { transTask ->
                        if (transTask.isSuccessful) {
                            val transDocs = transTask.result
                            val totalUsed = transDocs?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                            Log.d("BudgetViewModel", "Total used for ${limit.category} in $month: $totalUsed")
                            limit.usedAmount = totalUsed
                        } else {
                            Log.e("BudgetViewModel", "Failed to fetch transactions for categoryId: ${limit.categoryId} in $month", transTask.exception)
                            limit.usedAmount = 0.0 // Default to 0 if transaction fetch fails
                        }
                        limit // Return the limit object, potentially updated
                    }
                limitsWithUsageTasks.add(task)
            }

            if (limitsWithUsageTasks.isEmpty()) {
                _categoryLimits.value = emptyList()
                _isLoading.value = false // No tasks to process, stop loading
                Log.d("BudgetViewModel", "No usage tasks to process for $month.")
            } else {
                Tasks.whenAllSuccess<CategoryLimit>(limitsWithUsageTasks)
                    .addOnSuccessListener { updatedLimitsList ->
                        val validLimits = updatedLimitsList.filterNotNull()
                        Log.d("BudgetViewModel", "All limits usage computed for $month. Posting ${validLimits.size} to LiveData.")
                        _categoryLimits.value = validLimits
                        _isLoading.value = false // Successfully got data and calculated usage
                    }
                    .addOnFailureListener { failureException ->
                        Log.e("BudgetViewModel", "Failed to compute category usage for $month", failureException)
                        _categoryLimits.value = limitSnapshots.map { it.toObject(CategoryLimit::class.java).copy(id = it.id, usedAmount = 0.0) } // Post with 0 usage
                        _isLoading.value = false // Error in usage calculation, stop loading
                    }
            }
        }
    }

    // This function is a one-time load, not using listeners.
    // If used, it should also manage _isLoading.
    fun loadCategoryLimitsOneTime(month: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _categoryLimits.value = emptyList()
            _isLoading.value = false // Stop loading
            return
        }
        _isLoading.value = true // Start loading for one-time fetch
        viewModelScope.launch { // Use coroutines for cleaner async code
            try {
                val documents = firestore.collection("users").document(uid)
                    .collection("category_limits")
                    .whereEqualTo("month", month)
                    .get()
                    .await() // Suspend until data is fetched

                val limits = documents.mapNotNull {
                    it.toObject(CategoryLimit::class.java)?.copy(id = it.id)
                }
                _categoryLimits.value = limits // Update LiveData
                Log.d("BudgetViewModel", "One-time loadCategoryLimits fetched ${limits.size}")
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Failed to load category limits (one-time)", e)
                _categoryLimits.value = emptyList()
            } finally {
                _isLoading.value = false // Stop loading
            }
        }
    }

    fun loadCategoryLimits(month: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _categoryLimits.value = emptyList()
            return
        }

        firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("month", month)
            .get()
            .addOnSuccessListener { documents ->
                val limits = documents.mapNotNull {
                    it.toObject(CategoryLimit::class.java)?.copy(id = it.id)
                }
                Log.d("BudgetViewModel", "One-time loadCategoryLimits fetched ${limits.size}")
            }
            .addOnFailureListener { e ->
                Log.e("BudgetViewModel", "Failed to load category limits (one-time)", e)
            }
    }

    fun saveCategoryLimit(categoryLimit: CategoryLimit) {
        val uid = auth.currentUser?.uid ?: return

        val limitWithUserAndId = categoryLimit.copy(userId = uid) // Ensures userId is current


        // Because 'Activitybudget' now correctly sets categoryLimit.id for edits,
        // this 'if' condition will correctly identify an update.
        if (limitWithUserAndId.id.isNotEmpty()) {
            // THIS IS AN UPDATE
            Log.d("BudgetViewModel", "Updating existing category limit with ID: ${limitWithUserAndId.id}")
            firestore.collection("users").document(uid)
                .collection("category_limits")
                .document(limitWithUserAndId.id) // <<<< USES THE ORIGINAL DOCUMENT ID
                .set(limitWithUserAndId)      // Overwrites with all new data (including new category if changed)
                .addOnSuccessListener {
                    Log.d("BudgetViewModel", "Category limit updated successfully for ID: ${limitWithUserAndId.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("BudgetViewModel", "Failed to update category limit for ID: ${limitWithUserAndId.id}", e)
                }
        } else {
            // THIS IS A NEW ITEM (id was empty, e.g., from "Add Category Budget" button)
            Log.d("BudgetViewModel", "Saving new category limit for category: ${limitWithUserAndId.category}")
            firestore.collection("users").document(uid)
                .collection("category_limits")
                .add(limitWithUserAndId) // Firestore auto-generates an ID
                .addOnSuccessListener { documentReference ->
                    Log.d("BudgetViewModel", "New category limit saved with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("BudgetViewModel", "Failed to save new category limit", e)
                }
        }
    }

    fun deleteCategoryLimit(categoryLimit: CategoryLimit) {
        val uid = auth.currentUser?.uid ?: return
        if (categoryLimit.id.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .collection("category_limits")
                .document(categoryLimit.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("BudgetViewModel", "Category limit deleted: ${categoryLimit.id}")

                }
                .addOnFailureListener { e ->
                    Log.e("BudgetViewModel", "Failed to delete category limit", e)
                }
        } else {
            Log.w("BudgetViewModel", "Attempted to delete category limit with empty ID.")
        }
    }

    private fun getMonthDateRange(month: String): Pair<Date, Date> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(month) ?: Date()

        // Start of the month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.time

        // End of the month
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.time

        return Pair(start, end)
    }

    fun changeSelectedMonth(newMonth: String) {
        Log.d("BudgetViewModel", "Selected month changed to: $newMonth. Initiating data load.")
        _isLoading.value = true // Set loading to true when the month changes and we expect new data.

        // Detach old listeners before attaching new ones to prevent multiple listeners
        // for old months and ensure clean state.
        monthlyGoalListener?.remove()
        categoryLimitsListener?.remove()

        loadMonthlyGoal(newMonth) // This will set up its listener
        loadCategoryLimitsWithUsage(newMonth) // This will set up its listener and eventually set _isLoading to false
    }

    override fun onCleared() {
        super.onCleared()
        monthlyGoalListener?.remove()
        categoryLimitsListener?.remove()
        Log.d("BudgetViewModel", "ViewModel cleared, listeners removed.")
    }

}