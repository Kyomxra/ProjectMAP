package com.example.projectmap

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1001

    // Firestore
    private val firestore = FirebaseFirestore.getInstance()

    // untuk RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)

        // 🔹 Ambil userId dari SharedPreferences
        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        val userId = prefs.getString("userId", null)

        if (userId == null) {
            // Belum login
            tvWelcome.text = "Selamat siang,"
            tvUserName.text = "Guest"
        } else {
            // Sudah login, ambil data dari Firestore
            firestore.collection("User").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fName = document.getString("FName") ?: ""
                        val lName = document.getString("LName") ?: ""
                        tvWelcome.text = "Selamat siang,"
                        tvUserName.text = "$fName $lName"
                    } else {
                        tvWelcome.text = "Selamat siang,"
                        tvUserName.text = "User"
                    }
                }
                .addOnFailureListener {
                    tvWelcome.text = "Selamat siang,"
                    tvUserName.text = "User"
                }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // === Drawer Setup ===
        drawerLayout = view.findViewById(R.id.drawerLayout)
        navView = view.findViewById(R.id.navView)
        bottomNav = view.findViewById(R.id.bottomNavigation)
        val topAppBar = view.findViewById<MaterialToolbar>(R.id.topAppBar)

        (requireActivity() as AppCompatActivity).setSupportActionBar(topAppBar)

        // buka drawer kiri kalau tombol navigation diklik
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Listener Drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                }
                R.id.nav_settings -> Toast.makeText(requireContext(), "Settings clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> {
                    // Clear SharedPreferences
                    prefs.edit().remove("userId").apply()
                    Toast.makeText(requireContext(), "Logout berhasil!", Toast.LENGTH_SHORT).show()

                    // Hapus semua fragment di back stack biar nggak bisa balik
                    parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

                    // Ganti ke LoginFragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, LoginFragment())
                        .commit()
                }

            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Listener Bottom Nav
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, DashboardFragment())
                        .commit()
                    true
                }
                R.id.nav_search -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, LocationFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }

        // === Summary Fragment ===
        val summaryFragment =
            childFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment

        val income = 1_200_000
        val expense = 460_000
        summaryFragment.updateSummary("Agustus 2025", income, expense)

        // === RecyclerView Transaksi ===
        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        transactionList.addAll(
            listOf(
                Transaction("Makan Siang Mi Ayam", "- Rp 20,000", "Today"),
                Transaction("Transfer ke Fawwaz", "- Rp 75,000", "Yesterday"),
                Transaction("Top Up Valorant", "- Rp 350,000", "Aug 11, 2025")
            )
        )
        transactionAdapter = TransactionAdapter(transactionList)
        rvTransactions.adapter = transactionAdapter

        // === Floating Action Button ===
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddBottomSheet()
        }
    }

    // 🔹 ambil lokasi
    private fun getCurrentLocation(onResult: (String) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses =
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)

                val placeName = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    "Lokasi tidak diketahui"
                }
                onResult(placeName)
            } else {
                onResult("Lokasi tidak ditemukan")
            }
        }
    }

    // 🔹 bottom sheet: pilih pemasukan / pengeluaran
    private fun showAddBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add, null)
        bottomSheetDialog.setContentView(view)

        val btnPemasukan = view.findViewById<TextView>(R.id.btnPemasukan)
        val btnPengeluaran = view.findViewById<TextView>(R.id.btnPengeluaran)

        btnPemasukan.setOnClickListener {
            bottomSheetDialog.dismiss()
            showAddIncomeDialog()
        }

        btnPengeluaran.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(requireContext(), "Tambah Pengeluaran", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog.show()
    }

    // 🔹 dialog tambah pemasukan
    private fun showAddIncomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_income, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambahkan Pemasukan")
            .setView(dialogView)
            .create()

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddIncome)

        // Isi spinner tipe
        val types = listOf("Gaji", "Bonus", "Lainnya")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter

        // DatePicker
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
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
                getCurrentLocation { lokasi ->
                    val transaksi = Transaction(
                        title = type,
                        amount = "+ Rp $amount",
                        date = "$date – 📍$lokasi"
                    )

                    transactionList.add(0, transaksi)
                    transactionAdapter.notifyItemInserted(0)

                    Toast.makeText(requireContext(), "Pemasukan ditambahkan!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(requireContext(), "Lengkapi semua field!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
}
