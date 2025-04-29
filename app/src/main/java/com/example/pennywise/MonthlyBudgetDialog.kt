package com.example.pennywise.budget

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.example.pennywise.Category
import com.example.pennywise.CategoryLimit
import com.example.pennywise.R
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object MonthlyBudgetDialog {
    fun show(
        context: Context,
        onSave: (CategoryLimit) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_set_budget, null)
        val minEdit = view.findViewById<EditText>(R.id.editMin)
        val maxEdit = view.findViewById<EditText>(R.id.editMax)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)

        val db = AppDatabase.getDatabase(context)

        // Load categories in a coroutine
        CoroutineScope(Dispatchers.Main).launch {
            val categories = db.categoryDao().getAllCategories()
            val categoryNames = categories.map { it.name }

            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        AlertDialog.Builder(context)
            .setTitle("Set Budget for Category")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val selectedCategory = spinnerCategory.selectedItem?.toString() ?: return@setPositiveButton
                val min = minEdit.text.toString().toDoubleOrNull() ?: 0.0
                val max = maxEdit.text.toString().toDoubleOrNull() ?: 0.0

                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                val categoryLimit = CategoryLimit(
                    category = selectedCategory,
                    month = currentMonth,
                    limit = max // Or combine with min if needed
                )

                onSave(categoryLimit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
