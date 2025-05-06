package com.example.pennywise

import android.util.Log

class BudgetRepository(
    private val goalDao: BudgetGoalDao,
    private val limitDao: CategoryLimitDao
) {

    // Budget Goal Methods
    suspend fun saveMonthlyGoal(goal: BudgetGoal) = goalDao.insertBudgetGoal(goal)

    suspend fun getMonthlyGoal(month: String): BudgetGoal? =
        goalDao.getBudgetGoal(month)

    // Category Limit Methods
    suspend fun saveCategoryLimit(limit: CategoryLimit) = limitDao.insertCategoryLimit(limit)

    suspend fun getUpdatedCategoryLimits(month: String): List<CategoryLimit> {
        val limits = limitDao.getCategoryLimits(month)
        val updatedLimits = limits.map { limit ->
            val used = limitDao.getUsedAmountForCategory(month, limit.category) ?: 0.0
            Log.d("BudgetRepository", "Category: ${limit.category}, Used: $used, Month: $month")
            limit.copy(usedAmount = used)
        }
        return updatedLimits
    }

    suspend fun getCategoryLimits(month: String): List<CategoryLimit> {
        return limitDao.getCategoryLimits(month)
    }

    suspend fun getCategoryLimit(month: String, category: String): CategoryLimit? =
        limitDao.getCategoryLimit(month, category)

    suspend fun deleteCategoryLimit(limit: CategoryLimit) {
        limitDao.deleteCategoryLimit(limit.id)
    }

}
