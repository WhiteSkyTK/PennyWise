package com.example.pennywise

import android.animation.ValueAnimator
import android.util.Log
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

    private var lastPosition = -1  // Track last animated position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    fun getCategories(): List<Category> = categories

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        if (position < categories.size) { // Add bounds check
            val category = categories[position]
            // <<< LOG 5: About to bind a ViewHolder >>>
            Log.d("CategoryAdapter_OnBind", "Binding item at position $position: ${category.name}")
            holder.bind(category)
        } else {
            Log.e("CategoryAdapter_OnBind", "Position $position is out of bounds for categories size ${categories.size}")
        }
        // Animate slide-in from right and fade-in, only if scrolling down and new item
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

    override fun getItemCount(): Int = categories.size

    fun updateData(newList: List<Category>) {
        val oldSize = categories.size
        categories = newList
            .sortedWith(compareBy<Category> { it.type.lowercase() }
                .thenBy { it.name.lowercase() })
        // <<< LOG 6: updateData called >>>
        Log.i("CategoryAdapter_Data", "updateData called. Old size: $oldSize, New size: ${categories.size}. Categories: ${categories.map { it.name }}. Notifying change.")
        notifyDataSetChanged() // Consider more specific notifications if performance becomes an issue
    }

    fun updateTotals(newMap: Map<String, Double>) {
        // <<< LOG 7: updateTotals called >>>
        Log.i("CategoryAdapter_Totals", "updateTotals called. Received new usage map: $newMap. Current map keys: ${categoryUsageMap.keys}. Notifying change.")
        categoryUsageMap = newMap // Update the map
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(category: Category) {
            val name = itemView.findViewById<TextView>(R.id.categoryName)
            val type = itemView.findViewById<TextView>(R.id.categoryType)
            val amountUsedText = itemView.findViewById<TextView>(R.id.amountUsedText)
            val optionsIcon = itemView.findViewById<ImageView>(R.id.optionsIcon)

            name.text = category.name
            type.text = category.type

            // <<< LOG 8: Inside bind, what map and total are being used? >>>
            val currentCategoryUsageMap = categoryUsageMap // Capture the map instance at this moment for logging
            val total = categoryUsageMap[category.id] ?: 0.0
            Log.d("CategoryCheck", "Checking ${category.id} in usage map keys: ${categoryUsageMap.keys}")
            Log.d("Bind", "Category: ${category.name} (${category.id}) -> Usage: $total")
            Log.i("CategoryAdapter_BindDetail", "Binding '${category.name}' (ID: ${category.id}). Usage from map: $total. Current adapter's map keys: ${currentCategoryUsageMap.keys}")

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