package com.example.pennywise

class BudgetRepository(
    private val goalDao: BudgetGoalDao,
    private val limitDao: CategoryLimitDao
) {

    suspend fun saveMonthlyGoal(goal: BudgetGoal) = goalDao.insertBudgetGoal(goal)

    suspend fun getMonthlyGoal(month: String): BudgetGoal? =
        goalDao.getBudgetGoal(month)

    suspend fun saveCategoryLimit(limit: CategoryLimit) = limitDao.insertCategoryLimit(limit)

    suspend fun getCategoryLimits(month: String): List<CategoryLimit> =
        limitDao.getCategoryLimits(month)

    suspend fun getCategoryLimit(month: String, category: String): CategoryLimit? =
        limitDao.getCategoryLimit(month, category)
}
