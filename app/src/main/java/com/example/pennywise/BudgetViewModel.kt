package com.example.pennywise

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.QuerySnapshot

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
                Log.e("BudgetViewModel", "Category limits listen failed for month: $month", e)
                _categoryLimits.value = emptyList()
                return@addSnapshotListener
            }

            if (limitSnapshots == null|| limitSnapshots.isEmpty) {
                Log.d("BudgetViewModel", "No category limits found in snapshot for $month.")
                _categoryLimits.value = emptyList()
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
                _categoryLimits.value = emptyList() // No limits found, update LiveData
                Log.d("BudgetViewModel", "No category limits found for $month, posting empty list.")
            } else {
                Tasks.whenAllSuccess<CategoryLimit>(limitsWithUsageTasks)
                    .addOnSuccessListener { updatedLimitsList ->
                        // The list might contain nulls if some tasks failed in a specific way, filter them
                        val validLimits = updatedLimitsList.filterNotNull()
                        Log.d("BudgetViewModel", "All limits usage computed for $month. Posting ${validLimits.size} to LiveData.")
                        _categoryLimits.value = validLimits
                    }
                    .addOnFailureListener { failureException ->
                        Log.e("BudgetViewModel", "Failed to compute category usage for $month", failureException)
                        // Post existing limits without updated usage or an empty list as error handling
                        _categoryLimits.value = limitSnapshots.map { it.toObject(CategoryLimit::class.java).copy(id = it.id, usedAmount = 0.0) }
                    }
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
        val monthToReload = categoryLimit.month // Get month before potential modification

        firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("categoryId", categoryLimit.categoryId)
            .whereEqualTo("month", categoryLimit.month)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingDoc = querySnapshot.documents.firstOrNull()
                val idToUse = existingDoc?.id ?: firestore.collection("users").document(uid)
                    .collection("category_limits").document().id

                // Ensure the limit being saved has the correct ID and userId
                val limitToSave = categoryLimit.copy(id = idToUse, userId = uid)

                firestore.collection("users").document(uid)
                    .collection("category_limits")
                    .document(idToUse)
                    .set(limitToSave)
                    .addOnSuccessListener {
                        Log.d("BudgetViewModel", "Category limit saved for ${limitToSave.categoryId} in month ${limitToSave.month}")

                    }
                    .addOnFailureListener { e ->
                        Log.e("BudgetViewModel", "Failed to save category limit", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("BudgetViewModel", "Failed to check for existing category limit", e)
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
        Log.d("BudgetViewModel", "Selected month changed to: $newMonth")
        loadMonthlyGoal(newMonth)
        loadCategoryLimitsWithUsage(newMonth)
    }

    override fun onCleared() {
        super.onCleared()
        monthlyGoalListener?.remove()
        categoryLimitsListener?.remove()
        Log.d("BudgetViewModel", "ViewModel cleared, listeners removed.")
    }

}