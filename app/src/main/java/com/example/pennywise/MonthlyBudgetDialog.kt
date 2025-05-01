package com.example.pennywise.budget

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.pennywise.CategoryLimit
import com.example.pennywise.R
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MonthlyBudgetDialog {
    fun show(
        context: Context,
        month: String,
        existingLimit: CategoryLimit? = null,
        onSave: (CategoryLimit) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_set_budget, null)
        val minEdit = view.findViewById<EditText>(R.id.editMin)
        val maxEdit = view.findViewById<EditText>(R.id.editMax)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val expenseRadio = view.findViewById<RadioButton>(R.id.type_expense)
        val incomeRadio = view.findViewById<RadioButton>(R.id.type_income)
        val otherRadio = view.findViewById<RadioButton>(R.id.type_other)

        val db = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.Main).launch {
            val categories = db.categoryDao().getAllCategories()

            val updateSpinner = {
                val selectedType = when {
                    expenseRadio.isChecked -> "expense"
                    incomeRadio.isChecked -> "income"
                    otherRadio.isChecked -> "other"
                    else -> ""
                }

                val filtered = categories.filter { it.type == selectedType }
                val categoryNames = filtered.map { it.name }.sorted()

                val finalList = mutableListOf("Please select a category")
                finalList.addAll(categoryNames)

                val adapter = object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    finalList
                ) {
                    override fun isEnabled(position: Int): Boolean {
                        return position != 0
                    }

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent) as TextView
                        view.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                        return view
                    }
                }

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter

                // If editing
                if (existingLimit != null) {
                    val index = finalList.indexOf(existingLimit.category)
                    if (index != -1) {
                        spinnerCategory.setSelection(index)
                    }
                    minEdit.setText(existingLimit.minAmount.toString())
                    maxEdit.setText(existingLimit.maxAmount.toString())
                } else {
                    spinnerCategory.setSelection(0)
                }
            }

            // Radio listeners now go here
            expenseRadio.setOnCheckedChangeListener { _, isChecked -> if (isChecked) updateSpinner() }
            incomeRadio.setOnCheckedChangeListener { _, isChecked -> if (isChecked) updateSpinner() }
            otherRadio.setOnCheckedChangeListener { _, isChecked -> if (isChecked) updateSpinner() }

            // Pre-select type if editing
            if (existingLimit != null) {
                val matchingType = categories.find { it.name == existingLimit.category }?.type ?: "Expense"
                when (matchingType) {
                    "Expense" -> expenseRadio.isChecked = true
                    "Income" -> incomeRadio.isChecked = true
                    "Other" -> otherRadio.isChecked = true
                }
            } else {
                expenseRadio.isChecked = true
            }

            updateSpinner()

            AlertDialog.Builder(context)
                .setTitle(if (existingLimit != null) "Edit Category Budget" else "Set Category Budget")
                .setView(view)
                .setPositiveButton("Save") { _, _ ->
                    val selectedPosition = spinnerCategory.selectedItemPosition
                    if (selectedPosition == 0) {
                        Toast.makeText(context, "Please select a valid category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val selectedCategory = spinnerCategory.selectedItem?.toString() ?: return@setPositiveButton
                    val min = minEdit.text.toString().toDoubleOrNull() ?: 0.0
                    val max = maxEdit.text.toString().toDoubleOrNull() ?: 0.0

                    CoroutineScope(Dispatchers.Main).launch {
                        val usedAmount = db.transactionDao().getUsedAmountForCategory(month, selectedCategory)

                        val categoryLimit = CategoryLimit(
                            category = selectedCategory,
                            month = month,
                            minAmount = min,
                            maxAmount = max,
                            usedAmount = usedAmount
                        )

                        onSave(categoryLimit)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
