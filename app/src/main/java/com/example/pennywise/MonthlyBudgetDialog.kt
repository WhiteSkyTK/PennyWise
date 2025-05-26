package com.example.pennywise

import android.R.attr.id
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore

object MonthlyBudgetDialog {
    private val firestore = FirebaseFirestore.getInstance()

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
        val sharedPref = context.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("loggedInUserId", null)

        if (userId == null) {
            Toast.makeText(context, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }
        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val categoryList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.takeIf { it.type == "expense" }?.apply { id = doc.id }
                }.sortedBy { it.name }

                val finalList = mutableListOf<Category?>()
                finalList.add(null)  // Placeholder
                finalList.addAll(categoryList)

                val adapter =
                    object : ArrayAdapter<String>(context, R.layout.spinner_item, finalList.map {
                        it?.name ?: "Please select a category"
                    }) {
                        override fun isEnabled(position: Int) = position != 0
                        override fun getDropDownView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val view =
                                super.getDropDownView(position, convertView, parent) as TextView
                            val typedValue = TypedValue()
                            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                            val textColor = context.getColor(typedValue.resourceId)
                            view.setTextColor(if (position == 0) Color.GRAY else textColor)
                            return view
                        }
                    }

                spinnerCategory.adapter = adapter

                // If editing existing
                existingLimit?.let {
                    val index = finalList.indexOfFirst { cat -> cat?.name == it.category }
                    if (index >= 0) spinnerCategory.setSelection(index)
                    minEdit.setText(it.minAmount.toString())
                    maxEdit.setText(it.maxAmount.toString())
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
                                val selectedCategory = finalList[selectedPosition]!!
                                val categoryId = selectedCategory.id
                                val categoryName = selectedCategory.name
                                val docId = "${categoryId}_$month"

                                fetchUsedAmount(context, month, categoryId) { usedAmount ->
                                    val categoryLimit = CategoryLimit(
                                        category = categoryName,
                                        categoryId = categoryId,
                                        month = month,
                                        minAmount = min,
                                        maxAmount = max,
                                        usedAmount = usedAmount
                                    )

                                    firestore.collection("users")
                                        .document(userId)
                                        .collection("categoryLimits")
                                        .document(docId)
                                        .set(categoryLimit)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Budget saved", Toast.LENGTH_SHORT).show()
                                            onSave(categoryLimit)
                                            dialog.dismiss()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to save budget", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        }
                    }
                }

                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                dialog.window?.setBackgroundDrawable(ColorDrawable(typedValue.data))
                dialog.show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
    }

    // Fetch sum of transaction amounts for a given month and category from Firestore
    private fun fetchUsedAmount(
        context: Context,
        month: String,
        categoryId: String,
        callback: (Double) -> Unit
    ) {
        val loggedInUserId = getLoggedInUserId(context)

        firestore.collection("users")
            .document(loggedInUserId)
            .collection("transactions")
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("monthYear", month)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val totalUsed = querySnapshot.documents.sumOf {
                    it.getDouble("amount") ?: 0.0
                }
                callback(totalUsed)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch used amount", Toast.LENGTH_SHORT).show()
                callback(0.0)
            }
    }
    fun getLoggedInUserId(context: Context): String {
        val prefs = context.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        return prefs.getString("loggedInUserId", "") ?: ""
    }
}