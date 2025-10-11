package com.example.projectmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

class SummaryFragment : Fragment() {
    private lateinit var tvMonth: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvBalance: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_summary, container, false)

        tvMonth = view.findViewById(R.id.tvMonth)
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        tvBalance = view.findViewById(R.id.tvBalance)
        progressBar = view.findViewById(R.id.progressBar)

        return view
    }

    fun updateSummary(month: String, income: Int, expense: Int) {
        val balance = income - expense

        // Format currency dengan pemisah ribuan
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        tvMonth.text = month
        tvIncome.text = formatter.format(income).replace("Rp", "Rp ")
        tvExpense.text = formatter.format(expense).replace("Rp", "Rp ")

        // Format balance dengan + atau -
        val balanceFormatted = formatter.format(Math.abs(balance)).replace("Rp", "Rp ")
        tvBalance.text = if (balance >= 0) {
            "+ $balanceFormatted"
        } else {
            "- $balanceFormatted"
        }

        // Update warna balance sesuai positif/negatif
        if (balance >= 0) {
            tvBalance.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
        } else {
            tvBalance.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        }

        // Update progress bar: shows income percentage (green = income, red = expense)
        // If you have more income, bar will be more green
        val total = income + expense
        progressBar.progress = if (total > 0) {
            ((income.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }
}