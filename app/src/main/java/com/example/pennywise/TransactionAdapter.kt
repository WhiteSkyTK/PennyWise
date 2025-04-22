package com.example.pennywise

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.TransactionItem
import com.example.pennywise.Transaction

class TransactionAdapter(private var items: List<TransactionItem> = listOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionItem.Header -> TYPE_HEADER
            is TransactionItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d("Adapter", "Creating ViewHolder for type: $viewType")
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction_group, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_ENTRY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
                EntryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.setBackgroundColor(Color.YELLOW)

        when (val item = items[position]) {
            is TransactionItem.Header -> {
                Log.d("Adapter", "Binding Header: ${item.title}")

                (holder as HeaderViewHolder).bind(item)
            }
            is TransactionItem.Entry -> {
                Log.d("Adapter", "Binding Transaction: ${item.transaction.category} - R${item.transaction.amount}")
                (holder as EntryViewHolder).bind(item.transaction)
            }
        }
    }

    fun updateData(newItems: List<TransactionItem>) {
        Log.d("Adapter", "Updating adapter with ${newItems.size} items")
        items = newItems
        notifyDataSetChanged() // You can replace this later with DiffUtil for better performance
        newItems.forEachIndexed { index, item ->
            when (item) {
                is TransactionItem.Entry -> Log.d("Adapter", "Data[$index]: Entry - ${item.transaction.category}, R${item.transaction.amount}")
                is TransactionItem.Header -> Log.d("Adapter", "Data[$index]: Header - ${item.title}")
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: TransactionItem.Header) {
            itemView.findViewById<TextView>(R.id.groupHeader).text = item.title
        }
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(transaction: Transaction) {
            val categoryIcon = itemView.findViewById<ImageView>(R.id.categoryIcon)
            val categoryLetter = itemView.findViewById<TextView>(R.id.categoryLetter) // You'll add this below

            if (transaction.photoUri.isNullOrEmpty()) {
                categoryIcon.visibility = View.GONE
                categoryLetter.visibility = View.VISIBLE
                categoryLetter.text = when (transaction.type) {
                    "Income" -> "I"
                    "Expense" -> "E"
                    else -> "O"
                }
                categoryLetter.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        when (transaction.type) {
                            "Income" -> android.R.color.holo_green_dark
                            "Expense" -> android.R.color.holo_red_dark
                            else -> android.R.color.darker_gray
                        }
                    )
                )
            } else {
                categoryIcon.visibility = View.VISIBLE
                categoryLetter.visibility = View.GONE
                // Load your image here, e.g., Glide or other
            }
            itemView.findViewById<TextView>(R.id.transactionName).text = transaction.category
            itemView.findViewById<TextView>(R.id.transactionNote).text = transaction.description ?: ""
            itemView.findViewById<TextView>(R.id.transactionAmount).apply {
                text = "R${transaction.amount}"
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        when (transaction.type) {
                            "Income" -> android.R.color.holo_green_dark
                            "Expense" -> android.R.color.holo_red_dark
                            else -> android.R.color.darker_gray
                        }

                    )
                )
            }
        }
    }
}