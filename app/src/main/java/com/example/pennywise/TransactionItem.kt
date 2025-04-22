package com.example.pennywise

sealed class TransactionItem {
    data class Entry(val transaction: Transaction) : TransactionItem()
}