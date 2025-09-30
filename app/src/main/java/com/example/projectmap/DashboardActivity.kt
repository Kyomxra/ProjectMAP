package com.example.projectmap

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // === Drawer Setup ===
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        bottomNav = findViewById(R.id.bottomNavigation)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        setSupportActionBar(topAppBar)

        // buka drawer kiri kalau tombol navigation diklik
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Listener Drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Listener Bottom Nav
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
                    // TODO: ganti fragment ke HomeFragment
                }
                R.id.nav_search -> {
                    Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show()
                    // TODO: ganti fragment ke LocationFragment
                }
            }
            true
        }

        // === Summary Fragment ===
        val summaryFragment =
            supportFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment

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
            showAddIncomeDialog()
        }

        btnPengeluaran.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Tambah Pengeluaran", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog.show()
    }

    private fun showAddIncomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_income, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddIncome)

        // Isi spinner tipe
        val types = listOf("Gaji", "Bonus", "Lainnya")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter

        // DatePicker
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    etDate.setText("$day/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Button Tambahkan
        btnAdd.setOnClickListener {
            val type = spinnerType.selectedItem.toString()
            val date = etDate.text.toString()
            val amount = etAmount.text.toString()

            if (date.isNotEmpty() && amount.isNotEmpty()) {
                Toast.makeText(this, "Pemasukan $type: Rp$amount pada $date ditambahkan", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Lengkapi semua field!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}
