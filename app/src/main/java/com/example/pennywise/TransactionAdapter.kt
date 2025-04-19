package com.example.pennywise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.TransactionItem
import com.example.pennywise.Transaction


class TransactionAdapter(
    private val items: List<TransactionItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
        when (val item = items[position]) {
            is TransactionItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TransactionItem.Entry -> (holder as EntryViewHolder).bind(item.transaction)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: TransactionItem.Header) {
            itemView.findViewById<TextView>(R.id.groupHeader).text = item.title
        }
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(transaction: Transaction) {
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