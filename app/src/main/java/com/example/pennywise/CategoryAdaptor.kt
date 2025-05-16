package com.example.pennywise

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<Category>,
    private var categoryUsageMap: Map<String, Double>, // Ensure this is initialized correctly
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newList: List<Category>) {
        categories = newList
            .sortedWith(compareBy<Category> { it.type.lowercase() }
                .thenBy { it.name.lowercase() })
        notifyDataSetChanged()
    }

    fun updateTotals(newMap: Map<String, Double>) {
        categoryUsageMap = newMap
        notifyDataSetChanged()  // Notify that the totals have been updated
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(category: Category) {
            val name = itemView.findViewById<TextView>(R.id.categoryName)
            val type = itemView.findViewById<TextView>(R.id.categoryType)
            val amountUsedText = itemView.findViewById<TextView>(R.id.amountUsedText)
            val optionsIcon = itemView.findViewById<ImageView>(R.id.optionsIcon)

            name.text = category.name
            type.text = category.type

            val total = categoryUsageMap[category.name] ?: 0.0

            // Animate the amount used
            val animator = ValueAnimator.ofFloat(0f, total.toFloat())
            animator.duration = 1000  // 1 second
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                amountUsedText.text = "R %.2f used".format(animatedValue)
            }
            animator.start()

            optionsIcon.setOnClickListener {
                val popup = PopupMenu(itemView.context, optionsIcon)
                popup.menuInflater.inflate(R.menu.menu_category_options, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.edit_category -> {
                            onEdit(category)
                            true
                        }
                        R.id.delete_category -> {
                            onDelete(category)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}