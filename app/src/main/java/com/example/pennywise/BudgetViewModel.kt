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

    private val _categoryLimits = MutableLiveData<List<CategoryLimit>>()
    val categoryLimits: LiveData<List<CategoryLimit>> get() = _categoryLimits

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

    fun loadCategoryLimitsWithUsage(month: String) {
        viewModelScope.launch {
            _categoryLimits.value = repository.getUpdatedCategoryLimits(month)
        }
    }

    // Category Limits Handling
    fun loadCategoryLimits(month: String) {
        viewModelScope.launch {
            _categoryLimits.value = repository.getCategoryLimits(month)
        }
    }

    fun saveCategoryLimit(categoryLimit: CategoryLimit) {
        viewModelScope.launch {
            repository.saveCategoryLimit(categoryLimit)
            loadCategoryLimits(categoryLimit.month)  // Reload category limits after saving
        }
    }

    fun deleteCategoryLimit(categoryLimit: CategoryLimit) {
        viewModelScope.launch {
            repository.deleteCategoryLimit(categoryLimit)
            loadCategoryLimits(categoryLimit.month)  // Reload category limits after deletion
        }
    }
}
