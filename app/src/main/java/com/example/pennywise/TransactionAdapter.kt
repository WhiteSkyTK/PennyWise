package com.example.pennywise

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var items: List<TransactionItem> = listOf(),
    private val loggedInUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var lastPosition = -1

    companion object {
        private const val TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ENTRY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
                EntryViewHolder(view, loggedInUserId)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    fun getItems(): List<TransactionItem> = items

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EntryViewHolder) {
            val imageView = holder.itemView.findViewById<ImageView>(R.id.categoryIcon)
            Glide.with(holder.itemView.context).clear(imageView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionItem.Entry -> {
                (holder as EntryViewHolder).bind(item.transaction)
                setAnimation(holder.itemView, position)
            }
        }
    }

    fun updateData(newItems: List<TransactionItem>) {
        items = newItems
        lastPosition = -1
        Log.d("Adapter", "updateData called with ${items.size} items")
        notifyDataSetChanged() // Consider using DiffUtil for performance
        newItems.forEachIndexed { index, item ->
            if (item is TransactionItem.Entry) {
                Log.d("Adapter", "Data[$index]: ${item.transaction.category}, R${item.transaction.amount}")
            }
        }
    }

    private fun setAnimation(view: View, position: Int) {
        if (position > lastPosition) {
            view.alpha = 0f
            view.translationY = 100f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(position * 50L)
                .start()
            lastPosition = position
        }
    }

    class EntryViewHolder(itemView: View, private val loggedInUserId: String) : RecyclerView.ViewHolder(itemView) {
        fun bind(transaction: Transaction) {
            val normalizedType = transaction.type?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Other"
            Log.d("TransactionAdapter", "Normalized type: $normalizedType")

            val circleBackground = itemView.findViewById<ImageView>(R.id.circleBackground)
            val categoryIcon = itemView.findViewById<ImageView>(R.id.categoryIcon)
            val categoryLetter = itemView.findViewById<TextView>(R.id.categoryLetter)

            // Display date
            val transactionDateView = itemView.findViewById<TextView>(R.id.transactionDate)
            transactionDateView.text = formatTimestamp(transaction.date)

            // Background color logic
            val backgroundColorRes = when (normalizedType) {
                "Income" -> R.color.income_green
                "Expense" -> R.color.expense_red
                else -> R.color.gray
            }
            circleBackground.setColorFilter(ContextCompat.getColor(itemView.context, backgroundColorRes))

            // Handle photoUri (if used in the future, e.g., Firebase Storage)
            val photoUri = transaction.photoPath
            if (!photoUri.isNullOrEmpty()) {
                // If Firebase Storage used: Glide.with(itemView).load(photoUri).into(categoryIcon)
                categoryIcon.visibility = View.GONE
                categoryLetter.visibility = View.VISIBLE
                categoryLetter.text = transaction.category?.firstOrNull()?.uppercase() ?: "?"
            } else {
                categoryIcon.visibility = View.GONE
                categoryLetter.visibility = View.VISIBLE
                categoryLetter.text = transaction.category?.firstOrNull()?.uppercase() ?: "?"
            }
            categoryLetter.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))

            itemView.findViewById<TextView>(R.id.transactionName).text = transaction.category ?: "Unknown"
            itemView.findViewById<TextView>(R.id.transactionNote).text = transaction.description ?: ""

            val transactionAmountView = itemView.findViewById<TextView>(R.id.transactionAmount)
            transactionAmountView.text = when (normalizedType) {
                "Income" -> "R${transaction.amount}"
                "Expense" -> "-R${transaction.amount}"
                else -> "R${transaction.amount}"
            }

            // Color animation
            val startColor = Color.BLACK
            val endColor = ContextCompat.getColor(itemView.context, backgroundColorRes)
            ValueAnimator.ofArgb(startColor, endColor).apply {
                duration = 500
                addUpdateListener { animator ->
                    transactionAmountView.setTextColor(animator.animatedValue as Int)
                }
                start()
            }

            // Item click opens TransactionDetailActivity
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, TransactionDetailActivity::class.java).apply {
                    putExtra("transaction_id", transaction.id)
                    putExtra("userId", loggedInUserId)
                    putExtra("amount", transaction.amount)
                    putExtra("type", transaction.type)
                    putExtra("category", transaction.category)
                    putExtra("description", transaction.description)
                    putExtra("date", transaction.date)
                    putExtra("startTime", transaction.startTime)
                    putExtra("endTime", transaction.endTime)
                    putExtra("photoPath", transaction.photoPath)
                }
                context.startActivity(intent)
                if (context is Activity) {
                    context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
        }
        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
