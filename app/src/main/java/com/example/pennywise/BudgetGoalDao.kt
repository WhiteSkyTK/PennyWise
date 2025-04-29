package com.example.pennywise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BudgetGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(goal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE month = :month")
    suspend fun getBudgetGoal(month: String): BudgetGoal?
}
