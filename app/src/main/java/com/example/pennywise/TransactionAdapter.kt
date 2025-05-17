package com.example.pennywise

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Intent
import android.view.View

class TransactionAdapter(private var items: List<TransactionItem> = listOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                EntryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

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
        notifyDataSetChanged() // You can replace this later with DiffUtil for better performance
        newItems.forEachIndexed { index, item ->
            when (item) {
                is TransactionItem.Entry -> Log.d("Adapter", "Data[$index]: Entry - ${item.transaction.category}, R${item.transaction.amount}")            }
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
                .setStartDelay(position * 50L) // stagger effect
                .start()
            lastPosition = position
        }
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(transaction: Transaction) {
            val normalizedType = transaction.type?.lowercase()?.capitalize() ?: "Other"
            Log.d("TransactionAdapter", "Normalized type: $normalizedType")

            val circleBackground = itemView.findViewById<ImageView>(R.id.circleBackground)
            val categoryIcon = itemView.findViewById<ImageView>(R.id.categoryIcon)
            val categoryLetter = itemView.findViewById<TextView>(R.id.categoryLetter)

            // Set circle background tint based on transaction type
            circleBackground.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    when (normalizedType) {
                        "Income" -> R.color.income_green
                        "Expense" -> R.color.expense_red
                        else -> R.color.gray
                    }
                )
            )

            // Always show the letter, regardless of photoUri
            categoryIcon.visibility = View.GONE
            categoryLetter.visibility = View.VISIBLE
            categoryLetter.text = when (normalizedType) {
                "Income" -> "I"
                "Expense" -> "E"
                else -> "O"
            }
            categoryLetter.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))

            itemView.findViewById<TextView>(R.id.transactionName).text = transaction.category
            itemView.findViewById<TextView>(R.id.transactionNote).text = transaction.description ?: ""

            val transactionAmountView = itemView.findViewById<TextView>(R.id.transactionAmount)
            transactionAmountView.text = when (normalizedType) {
                "Income" -> "R${transaction.amount}"
                "Expense" -> "-R${transaction.amount}"
                else -> "R${transaction.amount}"
            }

            // Animate color change from black to the appropriate color
            val startColor = Color.BLACK
            val endColor = ContextCompat.getColor(
                itemView.context,
                when (normalizedType) {
                    "Income" -> R.color.income_green
                    "Expense" -> R.color.expense_red
                    else -> R.color.gray
                }
            )

            val colorAnimator = ValueAnimator.ofArgb(startColor, endColor)
            colorAnimator.duration = 500 // Duration of the animation (in ms)
            colorAnimator.addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                transactionAmountView.setTextColor(animatedColor)
            }
            colorAnimator.start()

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, TransactionDetailActivity::class.java).apply {
                    putExtra("transaction_id", transaction.id)
                    putExtra("amount", transaction.amount)
                    putExtra("type", transaction.type)
                    putExtra("category", transaction.category)
                    putExtra("description", transaction.description)
                    putExtra("date", transaction.date)
                    putExtra("startTime", transaction.startTime)
                    putExtra("endTime", transaction.endTime)
                    putExtra("photoUri", transaction.photoUri)
                }
                if (context is Activity) {
                    context.startActivity(intent)
                    // Add animation for activity open transition
                    context.overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                } else {
                    context.startActivity(intent)
                }
            }
        }
    }
}