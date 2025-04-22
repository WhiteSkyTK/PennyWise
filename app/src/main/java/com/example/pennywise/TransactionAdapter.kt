package com.example.pennywise

import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pennywise.TransactionItem
import com.example.pennywise.Transaction

class TransactionAdapter(private var items: List<TransactionItem> = listOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            }
        }
    }

    fun updateData(newItems: List<TransactionItem>) {
        items = newItems
        notifyDataSetChanged() // You can replace this later with DiffUtil for better performance
        newItems.forEachIndexed { index, item ->
            when (item) {
                is TransactionItem.Entry -> Log.d("Adapter", "Data[$index]: Entry - ${item.transaction.category}, R${item.transaction.amount}")            }
        }
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(transaction: Transaction) {
            val normalizedType = transaction.type?.lowercase()?.capitalize() ?: "Other"
            Log.d("TransactionAdapter", "Normalized type: $normalizedType") // Add this debug log

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

            if (transaction.photoUri.isNullOrEmpty()) {
                categoryIcon.visibility = View.GONE
                categoryLetter.visibility = View.VISIBLE
                categoryLetter.text = when (normalizedType) {
                    "Income" -> "I"
                    "Expense" -> "E"
                    else -> "O"
                }
                categoryLetter.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        android.R.color.white
                    )
                )
            } else {
                categoryIcon.visibility = View.VISIBLE
                categoryLetter.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(transaction.photoUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder)
                    .into(categoryIcon)
            }

            itemView.findViewById<TextView>(R.id.transactionName).text = transaction.category
            itemView.findViewById<TextView>(R.id.transactionNote).text = transaction.description ?: ""

            itemView.findViewById<TextView>(R.id.transactionAmount).apply {
                text = when (normalizedType) {
                    "Income" -> "R${transaction.amount}"
                    "Expense" -> "-R${transaction.amount}"
                    else -> "R${transaction.amount}"
                }
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        when (normalizedType) {
                            "Income" -> R.color.income_green
                            "Expense" -> R.color.expense_red
                            else -> R.color.gray
                        }
                    )
                )
            }
        }
    }
}