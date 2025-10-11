package com.example.projectmap

data class Transaction(
    val title: String,
    val amount: String,
    val date: String,
    val timestamp: Long = 0
)