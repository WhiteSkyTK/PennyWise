package com.example.pennywise.budget

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.CategoryLimit
import com.example.pennywise.R

class CategoryLimitAdapter(
    private var items: List<CategoryLimit>,
    private val onEdit: (CategoryLimit) -> Unit,
    private val onDelete: (CategoryLimit) -> Unit
) : RecyclerView.Adapter<CategoryLimitAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.categoryName)
        val minMaxText: TextView = view.findViewById(R.id.limitDetails)
        val maxText: TextView = view.findViewById(R.id.limitDetail)
        val currentText: TextView = view.findViewById(R.id.textCurrentUsage)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val optionsIcon: ImageView = view.findViewById(R.id.optionsIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_limit, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.categoryName.text = item.category
            holder.minMaxText.text = "Min: R${item.minAmount}"
            holder.maxText.text = "Max: R${item.maxAmount}"
            val usedAmount = item.usedAmount
            holder.currentText.text = "Used: R%.2f".format(usedAmount)

            val percentUsed = if (item.maxAmount > 0) ((usedAmount / item.maxAmount) * 100).toInt() else 0
            holder.progressBar.progress = percentUsed

        Log.d("CategoryLimitAdapter", "Category: ${item.category}, Used: $usedAmount, Max: ${item.maxAmount}, Progress: $percentUsed")

        holder.optionsIcon.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            popup.menuInflater.inflate(R.menu.menu_category_limit_options, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEdit(item)
                        true
                    }
                    R.id.action_delete -> {
                        onDelete(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    fun updateData(newItems: List<CategoryLimit>) {
        this.items = newItems
        notifyDataSetChanged() //notify the budget about changes
    }
}