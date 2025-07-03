package com.tk.pennywise

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context // Changed from Activity for broader use if needed, but Activity is fine if always from one
import android.content.Intent
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
// Removed: ListAdapter import
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val context: Context, // Using Context for flexibility, Activity is also fine
    private val loggedInUserId: String,
    private var items: List<TransactionItem> = listOf(),
    // Callbacks to MainActivity
    private val onSelectionModeChange: (isInSelectionMode: Boolean) -> Unit,
    private val onItemSelectionChanged: (selectedCount: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var lastPosition = -1 // For your existing animation

    // --- Selection State ---
    private val selectedTransactionIds = mutableSetOf<String>()
    var isSelectionModeActive: Boolean = false
        private set // 외부에서는 읽기만 가능하도록

    companion object {
        private const val TYPE_ENTRY = 1
        // Add other view types if TransactionItem has more variants
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionItem.Entry -> TYPE_ENTRY
            // Handle other item types if any
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ENTRY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
                // Pass selection handling logic to ViewHolder if needed, or handle in onBind directly
                EntryViewHolder(view, loggedInUserId, this) // Pass adapter for callbacks
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    fun getItems(): List<TransactionItem> = items

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EntryViewHolder) {
            // Your existing Glide clear logic
            val imageView = holder.itemView.findViewById<ImageView>(R.id.categoryIcon) // Ensure this ID is correct for Glide
            Glide.with(holder.itemView.context).clear(imageView)
            holder.cancelAmountAnimation() // Cancel ongoing animation for recycled view
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is TransactionItem.Entry -> {
                (holder as EntryViewHolder).bind(item.transaction)
                // Your existing animation call
                setAnimation(holder.itemView, position)
            }
            // Handle other item types if any
        }
    }

    fun updateData(newItems: List<TransactionItem>) {
        val oldSize = items.size
        items = newItems
        lastPosition = -1 // Reset for your animation
        Log.d("Adapter", "updateData called with ${newItems.size} items")

        // Crucial: If in selection mode, new data might mean selections are invalid.
        // Decide if you want to clear selections or try to maintain them (more complex).
        // For simplicity, let's clear selections when data is completely replaced.
        if (isSelectionModeActive) {
            clearSelectionsAndExitMode() // This will also call notifyDataSetChanged
        } else {
            notifyDataSetChanged() // Default behavior if not in selection mode
        }

        newItems.forEachIndexed { index, listItem ->
            if (listItem is TransactionItem.Entry) {
                Log.d("Adapter", "Data[$index]: ${listItem.transaction.category}, R${listItem.transaction.amount}")
            }
        }
    }

    private fun setAnimation(view: View, position: Int) {
        if (position > lastPosition) {
            view.alpha = 0f
            view.translationY = 100f // Slide from bottom
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400) // Keep your duration
                .setStartDelay(position * 50L) // Staggered delay
                .start()
            lastPosition = position
        }
    }

    // --- Public methods for selection management (called by ViewHolder or MainActivity) ---

    fun toggleSelection(transactionId: String) {
        val itemPosition = items.indexOfFirst { it is TransactionItem.Entry && it.transaction.id == transactionId }
        if (itemPosition == -1) return // Should not happen

        if (selectedTransactionIds.contains(transactionId)) {
            selectedTransactionIds.remove(transactionId)
        } else {
            selectedTransactionIds.add(transactionId)
        }
        onItemSelectionChanged(selectedTransactionIds.size) // Notify MainActivity of count change
        notifyItemChanged(itemPosition) // Update only this item's view for background change

        // If all items are deselected while in selection mode, exit selection mode
        if (isSelectionModeActive && selectedTransactionIds.isEmpty()) {
            exitSelectionModeInternal() // Internal call to avoid double notify
        }
    }

    fun enterSelectionMode(transactionId: String) {
        if (!isSelectionModeActive) {
            isSelectionModeActive = true
            onSelectionModeChange(true) // Notify MainActivity
            // Redraw all visible items to allow selection state to apply if needed initially
            // This is okay for a mode switch.
            // notifyDataSetChanged() // Can be too broad.
            // Instead, the first item selection will trigger its own notifyItemChanged.
        }
        toggleSelection(transactionId) // Select the item that initiated the mode
    }

    // Called by MainActivity to exit selection mode (e.g., after delete or cancel button)
    fun clearSelectionsAndExitMode() {
        if (isSelectionModeActive || selectedTransactionIds.isNotEmpty()) {
            selectedTransactionIds.clear()
            isSelectionModeActive = false
            onSelectionModeChange(false)
            onItemSelectionChanged(0)
            notifyDataSetChanged() // Redraw all to remove selection highlights
        }
    }

    private fun exitSelectionModeInternal() {
        // Used internally when the last item is deselected
        isSelectionModeActive = false
        onSelectionModeChange(false)
        onItemSelectionChanged(0) // Should be 0 here
        // No full notifyDataSetChanged here, as individual items handle their deselection UI update.
        // However, if the bulk action bar relies on a full refresh, consider it.
        // For now, assume MainActivity handles the UI change based on the callback.
    }


    fun getSelectedTransactionIds(): List<String> {
        return selectedTransactionIds.toList()
    }

    // --- ViewHolder for Transaction Entries ---
    // Make sure EntryViewHolder constructor takes the adapter instance
    class EntryViewHolder(
        itemView: View,
        private val loggedInUserId: String,
        private val adapter: TransactionAdapter // To access selection state and methods
    ) : RecyclerView.ViewHolder(itemView) {

        // Cache views for performance
        private val transactionNameTextView: TextView = itemView.findViewById(R.id.transactionName)
        private val transactionDateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        private val transactionNoteTextView: TextView = itemView.findViewById(R.id.transactionNote)
        private val transactionAmountTextView: TextView = itemView.findViewById(R.id.transactionAmount)
        private val circleBackgroundImageView: ImageView = itemView.findViewById(R.id.circleBackground)
        private val categoryIconImageView: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val categoryLetterTextView: TextView = itemView.findViewById(R.id.categoryLetter)
        private var currentAmountAnimator: ValueAnimator? = null


        fun bind(transaction: Transaction) {
            // Your existing binding logic
            val normalizedType = transaction.type?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Other"
            Log.d("TransactionAdapterVH", "Binding: ${transaction.id}, Type: $normalizedType")

            transactionDateTextView.text = formatTimestamp(transaction.date)

            val backgroundColorRes = when (normalizedType) {
                "Income" -> R.color.income_green
                "Expense" -> R.color.expense_red
                else -> R.color.gray
            }
            circleBackgroundImageView.setColorFilter(ContextCompat.getColor(itemView.context, backgroundColorRes))

            val photoUri = transaction.photoPath
            if (!photoUri.isNullOrEmpty()) {
                // Glide.with(itemView.context).load(photoUri).into(categoryIconImageView)
                categoryIconImageView.visibility = View.GONE // As per your original logic
                categoryLetterTextView.visibility = View.VISIBLE
                categoryLetterTextView.text = transaction.category?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            } else {
                categoryIconImageView.visibility = View.GONE
                categoryLetterTextView.visibility = View.VISIBLE
                categoryLetterTextView.text = transaction.category?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
            categoryLetterTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))

            transactionNameTextView.text = transaction.category ?: "Unknown"
            transactionNoteTextView.text = transaction.description ?: ""
            transactionNoteTextView.visibility = if (transaction.description.isNullOrEmpty()) View.GONE else View.VISIBLE


            transactionAmountTextView.text = when (normalizedType) {
                "Income" -> "R${"%.2f".format(transaction.amount)}"
                "Expense" -> "-R${"%.2f".format(transaction.amount)}"
                else -> "R${"%.2f".format(transaction.amount)}"
            }

            // Your existing color animation for amount
            currentAmountAnimator?.cancel() // Cancel previous before starting a new one
            val startColor = transactionAmountTextView.currentTextColor // Animate from current color if preferred
            // val startColor = Color.BLACK // Or always from black as in your original
            val endColor = ContextCompat.getColor(itemView.context, backgroundColorRes)

            currentAmountAnimator = ValueAnimator.ofArgb(startColor, endColor).apply {
                duration = 500L
                addUpdateListener { animator ->
                    transactionAmountTextView.setTextColor(animator.animatedValue as Int)
                }
                start()
            }

            val context = itemView.context

            // --- Handle Selection UI and Click Listeners ---
            if (adapter.selectedTransactionIds.contains(transaction.id)) {
                // Item is selected
                val typedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.selectedItemBackground, typedValue, true)
                itemView.setBackgroundColor(typedValue.data)
            } else {
                val defaultBgTypedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.colorBackground, defaultBgTypedValue, true) // Or your item's specific background attribute
                itemView.setBackgroundColor(defaultBgTypedValue.data)
            }

            itemView.setOnClickListener {
                if (adapter.isSelectionModeActive) {
                    adapter.toggleSelection(transaction.id)
                } else {
                    // Normal click: Open TransactionDetailActivity
                    val intent = Intent(itemView.context, TransactionDetailActivity::class.java).apply {
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
                    itemView.context.startActivity(intent)
                    if (itemView.context is Activity) {
                        (itemView.context as Activity).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (!adapter.isSelectionModeActive) {
                    adapter.enterSelectionMode(transaction.id)
                } else {
                    // If already in selection mode, a long press could perhaps do something else,
                    // or just toggle selection like a normal click. For simplicity, let's make it toggle.
                    adapter.toggleSelection(transaction.id)
                }
                true // Consume the long click
            }
        }

        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        fun cancelAmountAnimation() {
            currentAmountAnimator?.cancel()
        }
    }
}