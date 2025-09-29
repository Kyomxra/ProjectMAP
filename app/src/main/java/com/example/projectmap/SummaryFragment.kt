package com.example.projectmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import android.widget.TextView

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

    fun updateSummary(month: String, income: Int, expense: Int){
        val balance = income - expense
        tvMonth.text = month
        tvIncome.text = "Rp $income"
        tvExpense.text = "Rp $expense"
        tvBalance.text = if (balance >= 0) "+ Rp $balance" else "- Rp ${-balance}"

        val total = income + expense
        progressBar.progress = if (total > 0) (income * 100) / total else 0
    }
}