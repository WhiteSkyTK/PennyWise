package com.tk.pennywise


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BudgetRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("User not logged in")

    // Save Budget Goal
    suspend fun saveMonthlyGoal(goal: BudgetGoal) {
        firestore.collection("users").document(uid)
            .collection("budget_goals")
            .document(goal.month)
            .set(goal)
            .await()
    }

    // Get Monthly Goal
    suspend fun getMonthlyGoal(month: String): BudgetGoal? {
        val snapshot = firestore.collection("users").document(uid)
            .collection("budget_goals")
            .document(month)
            .get()
            .await()

        return snapshot.toObject(BudgetGoal::class.java)
    }

    // Save Category Limit
    suspend fun saveCategoryLimit(limit: CategoryLimit) {
        val docId = if (limit.id.isBlank()) {
            firestore.collection("users").document(uid)
                .collection("category_limits")
                .document().id
        } else limit.id

        val toSave = limit.copy(id = docId, userId = uid)

        firestore.collection("users").document(uid)
            .collection("category_limits")
            .document(docId)
            .set(toSave)
            .await()
    }

    // Get all category limits for a month
    suspend fun getCategoryLimits(month: String): List<CategoryLimit> {
        val snapshot = firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("month", month)
            .get()
            .await()

        return snapshot.map { doc ->
            doc.toObject(CategoryLimit::class.java).copy(id = doc.id)
        }
    }

    // Get a specific category limit
    suspend fun getCategoryLimit(month: String, categoryId: String): CategoryLimit? {
        val snapshot = firestore.collection("users").document(uid)
            .collection("category_limits")
            .whereEqualTo("month", month)
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let {
            it.toObject(CategoryLimit::class.java)?.copy(id = it.id)
        }
    }

    // Delete a category limit
    suspend fun deleteCategoryLimit(limit: CategoryLimit) {
        firestore.collection("users").document(uid)
            .collection("category_limits")
            .document(limit.id)
            .delete()
            .await()
    }

    // Get category limits + calculate usedAmount from transactions
    suspend fun getUpdatedCategoryLimits(month: String): List<CategoryLimit> {
        val limits = getCategoryLimits(month)

        val updated = limits.map { limit ->
            val transSnapshot = firestore.collection("users").document(uid)
                .collection("transactions")
                .whereEqualTo("categoryId", limit.categoryId)
                .whereEqualTo("month", month)
                .get()
                .await()

            val totalUsed = transSnapshot.sumOf { it.getDouble("amount") ?: 0.0 }

            Log.d("BudgetRepository", "CategoryId: ${limit.categoryId}, Used: $totalUsed, Month: $month")

            limit.copy(usedAmount = totalUsed)
        }

        return updated
    }
}
