package com.tk.pennywise


import androidx.room.*

@Dao
interface BudgetGoalDao {
    @Query("SELECT * FROM budget_goals")
    suspend fun getAll(): List<BudgetGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: BudgetGoal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<BudgetGoal>)

    @Query("DELETE FROM budget_goals")
    suspend fun deleteAll()
}
