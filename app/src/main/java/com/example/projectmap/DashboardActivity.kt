package com.example.projectmap

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvIncome = findViewById<TextView>(R.id.tvIncome)
        val tvExpense = findViewById<TextView>(R.id.tvExpense)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)

        // Dummy data
        val income = 1_200_000
        val expense = 460_000
        val balance = income - expense

        // Update UI
        tvIncome.text = "Rp ${income}"
        tvExpense.text = "Rp ${expense}"
        tvBalance.text = "+ Rp ${balance}"

        // Progress bar → persen pemasukan dibanding total
        val total = income + expense
        if (total > 0) {
            val progress = (income * 100) / total
            progressBar.progress = progress
        }

        // RecyclerView setup
        rvTransactions.layoutManager = LinearLayoutManager(this)
        val dummyTransactions = listOf(
            Transaction("Makan Siang Mi Ayam", "- Rp 20,000", "Today"),
            Transaction("Transfer ke Fawwaz", "- Rp 75,000", "Yesterday"),
            Transaction("Top Up Dana", "- Rp 350,000", "Aug 11, 2025")
        )
        rvTransactions.adapter = TransactionAdapter(dummyTransactions)
    }
}
