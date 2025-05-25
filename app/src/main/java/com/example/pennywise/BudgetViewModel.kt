package com.example.pennywise

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.QuerySnapshot

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()

    private val _monthlyGoal = MutableLiveData<BudgetGoal?>()
    val monthlyGoal: LiveData<BudgetGoal?> get() = _monthlyGoal

    private val _categoryLimits = MutableLiveData<List<CategoryLimit>>()
    val categoryLimits: LiveData<List<CategoryLimit>> get() = _categoryLimits

    fun loadMonthlyGoal(month: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("budget_goals")
            .whereEqualTo("month", month)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val goal = documents.first().toObject(BudgetGoal::class.java)
                    _monthlyGoal.value = goal
                } else {
                    _monthlyGoal.value = null
                }
            }
    }

    fun saveMonthlyGoal(goal: BudgetGoal) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("budget_goals")
            .document(goal.month)
            .set(goal)
            .addOnSuccessListener {
                _monthlyGoal.value = goal
            }
    }

    fun loadCategoryLimitsWithUsage(month: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("BudgetViewModel", "User not authenticated")
            return
        }

        Log.d("BudgetViewModel", "Fetching category limits for user: $uid and month: $month")

        firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("month", month)
            .get()
            .addOnSuccessListener { limitDocs ->
                Log.d("BudgetViewModel", "Fetched ${limitDocs.size()} category limit docs")

                val transactionTasks = mutableListOf<com.google.android.gms.tasks.Task<CategoryLimit>>()

                for (doc in limitDocs) {
                    val limit = doc.toObject(CategoryLimit::class.java).copy(id = doc.id)
                    Log.d("BudgetViewModel", "Processing limit: ${limit.category} (${limit.categoryId})")

                    val task = firestore.collection("users").document(uid)
                        .collection("transactions")
                        .whereEqualTo("categoryId", limit.categoryId)
                        .whereEqualTo("monthYear", month) // Make sure you're filtering with 'monthYear', not just 'month'
                        .get()
                        .continueWith { transTask ->
                            if (transTask.isSuccessful) {
                                val transDocs = transTask.result
                                val totalUsed = transDocs?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                                Log.d("BudgetViewModel", "Total used for ${limit.category}: $totalUsed")

                                limit.usedAmount = totalUsed
                            } else {
                                Log.e("BudgetViewModel", "Failed to fetch transactions for categoryId: ${limit.categoryId}", transTask.exception)
                            }

                            limit
                        }

                    transactionTasks.add(task)
                }

                Tasks.whenAllSuccess<CategoryLimit>(transactionTasks)
                    .addOnSuccessListener { updatedLimits ->
                        Log.d("BudgetViewModel", "All limits updated successfully. Posting ${updatedLimits.size} to LiveData.")
                        _categoryLimits.value = updatedLimits
                    }
                    .addOnFailureListener { e ->
                        Log.e("BudgetViewModel", "Failed to compute category usage", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("BudgetViewModel", "Failed to fetch category limits", e)
            }
    }

    fun loadCategoryLimits(month: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("month", month)
            .get()
            .addOnSuccessListener { documents ->
                val limits = documents.map {
                    it.toObject(CategoryLimit::class.java).copy(id = it.id)
                }
                _categoryLimits.value = limits
            }
    }

    fun saveCategoryLimit(categoryLimit: CategoryLimit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Check if a limit for this category and month already exists
        firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("categoryId", categoryLimit.categoryId)
            .whereEqualTo("month", categoryLimit.month)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingDoc = querySnapshot.documents.firstOrNull()
                val id = existingDoc?.id ?: firestore.collection("users").document(uid)
                    .collection("category_limits").document().id

                val limitToSave = categoryLimit.copy(id = id, userId = uid)

                firestore.collection("users").document(uid)
                    .collection("category_limits")
                    .document(id)
                    .set(limitToSave)
                    .addOnSuccessListener {
                        loadCategoryLimitsWithUsage(categoryLimit.month)
                    }
            }
    }

    fun deleteCategoryLimit(categoryLimit: CategoryLimit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (categoryLimit.id.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .collection("category_limits")
                .document(categoryLimit.id)
                .delete()
                .addOnSuccessListener {
                    loadCategoryLimits(categoryLimit.month)
                }
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

}