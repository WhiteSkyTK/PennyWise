package com.example.pennywise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(db.budgetGoalDao(), db.categoryLimitDao())

    private val _monthlyGoal = MutableLiveData<BudgetGoal?>()
    val monthlyGoal: LiveData<BudgetGoal?> get() = _monthlyGoal

    fun loadMonthlyGoal(month: String) {
        viewModelScope.launch {
            _monthlyGoal.value = repository.getMonthlyGoal(month)
        }
    }

    fun saveMonthlyGoal(goal: BudgetGoal) {
        viewModelScope.launch {
            repository.saveMonthlyGoal(goal)
            _monthlyGoal.value = goal
        }
    }

    // Add similar LiveData and methods for category limits if needed
}
