package com.example.projectmap

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // === Drawer Setup ===
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        setSupportActionBar(topAppBar)

        // buka drawer kiri kalau tombol navigation diklik
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // handle klik menu drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // === Existing Dashboard Logic ===
        val tvIncome = findViewById<TextView>(R.id.tvIncome)
        val tvExpense = findViewById<TextView>(R.id.tvExpense)
        val tvBalance = findViewById<TextView>(R.id.tvBalance)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)

        val income = 1_200_000
        val expense = 460_000
        val balance = income - expense

        tvIncome.text = "Rp $income"
        tvExpense.text = "Rp $expense"
        tvBalance.text = "+ Rp $balance"

        val total = income + expense
        if (total > 0) {
            val progress = (income * 100) / total
            progressBar.progress = progress
        }

        rvTransactions.layoutManager = LinearLayoutManager(this)
        val dummyTransactions = listOf(
            Transaction("Makan Siang Mi Ayam", "- Rp 20,000", "Today"),
            Transaction("Transfer ke Fawwaz", "- Rp 75,000", "Yesterday"),
            Transaction("Top Up Valorant", "- Rp 350,000", "Aug 11, 2025")
        )
        rvTransactions.adapter = TransactionAdapter(dummyTransactions)
    }
}
