package com.example.pennywise

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        val db = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.Main).launch {
            val categories = db.categoryDao().getAllCategories()
            val filtered = categories.filter { it.type.equals("expense", ignoreCase = true) }
            val categoryNames = filtered.map { it.name }.sorted()

            val finalList = mutableListOf("Please select a category")
            finalList.addAll(categoryNames)

            val adapter = object : ArrayAdapter<String>(
                context,
                R.layout.spinner_item, // your custom layout
                finalList
            ) {
                override fun isEnabled(position: Int): Boolean {
                    return position != 0
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    val typedValue = TypedValue()
                    val theme = context.theme
                    val color = if (theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true)) {
                        typedValue.data
                    } else {
                        Color.BLACK // fallback color if attribute not found
                    }

                    view.setTextColor(
                        if (position == 0) Color.GRAY else color
                    )
                    return view
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter

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

        val dialog = AlertDialog.Builder(context)
            .setTitle(if (existingLimit != null) "Edit Category Budget" else "Set Category Budget")
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val selectedPosition = spinnerCategory.selectedItemPosition
                val minText = minEdit.text.toString()
                val maxText = maxEdit.text.toString()

                val min = minText.toDoubleOrNull()
                val max = maxText.toDoubleOrNull()

                // Validations
                when {
                    selectedPosition == 0 -> {
                        Toast.makeText(context, "Please select a valid category.", Toast.LENGTH_SHORT).show()
                    }
                    min == null || min < 0 -> {
                        Toast.makeText(context, "Enter a valid non-negative minimum amount.", Toast.LENGTH_SHORT).show()
                    }
                    max == null || max < 0 -> {
                        Toast.makeText(context, "Enter a valid non-negative maximum amount.", Toast.LENGTH_SHORT).show()
                    }
                    min > max -> {
                        Toast.makeText(context, "Minimum amount cannot be greater than maximum.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // All good, proceed
                        val selectedCategory = spinnerCategory.selectedItem.toString()
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
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(typedValue.data))

        dialog.show()
    }
}