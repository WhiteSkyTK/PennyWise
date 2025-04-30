package com.example.pennywise

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

    suspend fun getCategoryLimits(month: String): List<CategoryLimit> =
        limitDao.getCategoryLimits(month)

    suspend fun getCategoryLimit(month: String, category: String): CategoryLimit? =
        limitDao.getCategoryLimit(month, category)

    suspend fun deleteCategoryLimit(limit: CategoryLimit) {
        limitDao.deleteCategoryLimit(limit.id)
    }
}
