package com.example.pennywise

sealed class TransactionItem {
    data class Header(val title: String) : TransactionItem()
    data class Entry(val transaction: Transaction) : TransactionItem()
}