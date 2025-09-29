package com.example.projectmap

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_search -> Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }


        // === Summary Fragment ===
        val summaryFragment =
            supportFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment

        // Dummy data
        val income = 1_200_000
        val expense = 460_000
        summaryFragment.updateSummary("Agustus 2025", income, expense)

        // === RecyclerView Transaksi ===
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        val dummyTransactions = listOf(
            Transaction("Makan Siang Mi Ayam", "- Rp 20,000", "Today"),
            Transaction("Transfer ke Fawwaz", "- Rp 75,000", "Yesterday"),
            Transaction("Top Up Valorant", "- Rp 350,000", "Aug 11, 2025")
        )
        rvTransactions.adapter = TransactionAdapter(dummyTransactions)

        // === Floating Action Button ===
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddBottomSheet()
        }
    }

    private fun showAddBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add, null)
        bottomSheetDialog.setContentView(view)

        val btnPemasukan = view.findViewById<TextView>(R.id.btnPemasukan)
        val btnPengeluaran = view.findViewById<TextView>(R.id.btnPengeluaran)

        btnPemasukan.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tambah Pemasukan", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(this, FormPemasukanActivity::class.java))
        }

        btnPengeluaran.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tambah Pengeluaran", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(this, FormPengeluaranActivity::class.java))
        }

        bottomSheetDialog.show()
    }
}
