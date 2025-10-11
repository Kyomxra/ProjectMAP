package com.example.projectmap

data class Transaction(
    val title: String,
    val amount: String,
    val date: String,
    val timestamp: Long = 0
)

data class TransactionWithLocation(
    val id: String = "",
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val amount: Double,
    val category: String,
    val date: String,
    val userId: String = "",
    val type: String = "expense"
)