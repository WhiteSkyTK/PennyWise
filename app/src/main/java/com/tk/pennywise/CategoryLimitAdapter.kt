package com.tk.pennywise

import android.animation.ValueAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

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

    private var lastPosition = -1  // add this at adapter class level
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.categoryName.text = item.category
        holder.minMaxText.text = "Min: R${item.minAmount}"
        holder.maxText.text = "Max: R${item.maxAmount}"
        val usedAmount = item.usedAmount
        animateCount(holder.currentText, 0.0, usedAmount)

        val percentUsed = if (item.maxAmount > 0) ((usedAmount / item.maxAmount) * 100).toInt() else 0
        holder.progressBar.progress = percentUsed.coerceIn(0, 100) // ensure 0-100

        // Color coding based on usage vs min/max
        val context = holder.itemView.context
        when {
            usedAmount < item.minAmount -> {
                holder.progressBar.progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_yellow)
                holder.currentText.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow))
            }
            usedAmount >= item.maxAmount -> {
                holder.progressBar.progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_red)
                holder.currentText.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
            }
            else -> {
                holder.progressBar.progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_green)
                holder.currentText.setTextColor(ContextCompat.getColor(context, R.color.green_400))
            }
        }

        Log.d("CategoryLimitAdapter", "Category: ${item.categoryId}, Used: $usedAmount, Max: ${item.maxAmount}, Progress: $percentUsed")

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
        // Use adapterPosition instead of position
        val currentPosition = holder.adapterPosition
        if (currentPosition != RecyclerView.NO_POSITION && currentPosition > lastPosition) {
            holder.itemView.alpha = 0f
            holder.itemView.translationX = 100f
            holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .start()
            lastPosition = currentPosition
        }
    }

    fun updateData(newItems: List<CategoryLimit>) {
        this.items = newItems.sortedBy { it.category.lowercase() } // Sort by category name alphabetically (case-insensitive)
        notifyDataSetChanged()
    }

    private fun animateCount(view: TextView, from: Double, to: Double) {
        val duration = 1000L
        val animator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat())
        animator.duration = duration
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            view.text = "R%.2f".format(value)
        }
        animator.start()
    }
}