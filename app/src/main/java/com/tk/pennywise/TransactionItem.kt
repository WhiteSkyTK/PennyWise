package com.tk.pennywise

sealed class TransactionItem {
    data class Entry(val transaction: Transaction) : TransactionItem()
}