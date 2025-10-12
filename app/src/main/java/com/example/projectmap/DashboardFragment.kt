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
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1001

    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)

        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        userId = prefs.getString("userId", null)

        if (userId == null) {
            tvWelcome.text = "Selamat siang,"
            tvUserName.text = "Guest"
            Toast.makeText(requireContext(), "Belum login! Silakan login dulu.", Toast.LENGTH_LONG).show()
        } else {
            android.util.Log.d("DashboardDebug", "User logged in with ID: $userId")
            loadUserData(userId!!, tvWelcome, tvUserName)
            loadTransactions(userId!!)
            loadSummary(userId!!)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

// Warm up GPS
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    android.util.Log.d("DashboardDebug", "GPS warming up, requesting location updates")
                    // Request location to warm up GPS
                    val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                        priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
                        interval = 10000
                        fastestInterval = 5000
                    }
                    try {
                        fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {}, null)
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardDebug", "Error warming up GPS: ${e.message}")
                    }
                } else {
                    android.util.Log.d("DashboardDebug", "GPS ready at: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        drawerLayout = view.findViewById(R.id.drawerLayout)
        navView = view.findViewById(R.id.navView)
        bottomNav = view.findViewById(R.id.bottomNavigation)
        val topAppBar = view.findViewById<MaterialToolbar>(R.id.topAppBar)

        (requireActivity() as AppCompatActivity).setSupportActionBar(topAppBar)

        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

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
                    prefs.edit().remove("userId").apply()
                    Toast.makeText(requireContext(), "Logout berhasil!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, LoginFragment())
                        .commit()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

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

        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(transactionList)
        rvTransactions.adapter = transactionAdapter

        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddBottomSheet()
        }

        // Target Tabungan button
        val btnTargetTabungan = view.findViewById<androidx.cardview.widget.CardView>(R.id.btnTargetTabungan)
        btnTargetTabungan?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GoalsFragment())
                .addToBackStack(null)
                .commit()
        }

        // button pendapatan pokok

        val btnPendapatanPokok = view.findViewById<androidx.cardview.widget.CardView>(R.id.btnPendapatanPokok)
        btnPendapatanPokok?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RecurringIncomeFragment())
                .addToBackStack(null)
                .commit()
        }

        val btnLaporanTransaksi = view.findViewById<androidx.cardview.widget.CardView>(R.id.btnLaporanTransaksi)
        btnLaporanTransaksi?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ReportFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadUserData(uid: String, tvWelcome: TextView, tvUserName: TextView) {
        firestore.collection("User").document(uid)
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

    override fun onResume() {
        super.onResume()
        userId?.let {
            android.util.Log.d("DashboardDebug", "onResume: refreshing data")
            loadTransactions(it)
            loadSummary(it)
            checkAndAddRecurringIncome(it)  // ← AUTO CHECK
        }
    }

    private fun loadTransactions(uid: String) {
        android.util.Log.d("DashboardDebug", "=== LOADING TRANSACTIONS ===")
        android.util.Log.d("DashboardDebug", "User ID: $uid")

        // Removed orderBy to avoid needing composite index
        firestore.collection("Transactions")
            .whereEqualTo("user_id", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("DashboardDebug", "Listen failed: ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    android.util.Log.d("DashboardDebug", "Snapshots null")
                    return@addSnapshotListener
                }

                android.util.Log.d("DashboardDebug", "Found ${snapshots.size()} documents")

                transactionList.clear()

                val tempList = mutableListOf<Transaction>()

                for (doc in snapshots) {
                    val type = doc.getString("type") ?: ""
                    val category = doc.getString("category") ?: "Transaksi"
                    val amount = doc.getLong("amount") ?: 0
                    val timestamp = doc.getTimestamp("date")
                    val note = doc.getString("note") ?: ""

                    val dateStr = if (timestamp != null) {
                        formatDate(timestamp)
                    } else {
                        "Unknown date"
                    }

                    val amountStr = formatCurrency(amount, type)
                    val title = if (note.isNotEmpty()) note else category

                    tempList.add(Transaction(title, amountStr, dateStr, timestamp?.toDate()?.time ?: 0))
                }

                // Sort in memory by date (newest first)
                tempList.sortByDescending { it.timestamp }
                transactionList.addAll(tempList)

                android.util.Log.d("DashboardDebug", "Total in list: ${transactionList.size}")
                transactionAdapter.notifyDataSetChanged()
            }
    }

    private fun loadSummary(uid: String) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val startOfMonth = Timestamp(calendar.time)

        calendar.set(currentYear, currentMonth + 1, 1, 0, 0, 0)
        val endOfMonth = Timestamp(calendar.time)

        android.util.Log.d("DashboardDebug", "Loading summary for userId: $uid")
        android.util.Log.d("DashboardDebug", "Month range: $startOfMonth to $endOfMonth")

        firestore.collection("Transactions")
            .whereEqualTo("user_id", uid)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("DashboardDebug", "Found ${documents.size()} transactions")
                var totalIncome = 0L
                var totalExpense = 0L

                for (doc in documents) {
                    val type = doc.getString("type") ?: ""
                    val amount = doc.getLong("amount") ?: 0

                    android.util.Log.d("DashboardDebug", "Transaction: type=$type, amount=$amount")

                    when (type) {
                        "income" -> totalIncome += amount
                        "expense" -> totalExpense += amount
                        "saving" -> totalExpense += amount
                    }
                }

                android.util.Log.d("DashboardDebug", "Total Income: $totalIncome, Total Expense: $totalExpense")

                val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
                val summaryFragment = childFragmentManager.findFragmentById(R.id.summaryFragment) as? SummaryFragment
                summaryFragment?.updateSummary(monthName, totalIncome.toInt(), totalExpense.toInt())
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DashboardDebug", "Error loading summary: ${e.message}")
                Toast.makeText(requireContext(), "Gagal memuat ringkasan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDate(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        return when {
            isSameDay(date, today) -> "Today"
            isSameDay(date, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(date)
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatCurrency(amount: Long, type: String): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formatted = formatter.format(amount).replace("Rp", "Rp ")
        return when (type) {
            "income" -> "+ $formatted"
            "expense" -> "- $formatted"
            "saving" -> "💰 $formatted"
            else -> formatted
        }
    }

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
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
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

    private fun showAddBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add, null)
        bottomSheetDialog.setContentView(view)

        val btnPemasukan = view.findViewById<TextView>(R.id.btnPemasukan)
        val btnPengeluaran = view.findViewById<TextView>(R.id.btnPengeluaran)
        val btnTabungan = view.findViewById<TextView>(R.id.btnTabungan)

        btnPemasukan.setOnClickListener {
            bottomSheetDialog.dismiss()
            showAddIncomeDialog()
        }

        btnPengeluaran.setOnClickListener {
            bottomSheetDialog.dismiss()
            showAddExpenseDialog()
        }

        btnTabungan?.setOnClickListener {
            bottomSheetDialog.dismiss()
            showAddSavingDialog()
        }

        bottomSheetDialog.show()
    }

    private fun showAddIncomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_income, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambahkan Pemasukan")
            .setView(dialogView)
            .create()

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddIncome)

        val types = listOf("Gaji", "Bonus", "Investasi", "Lainnya")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter

        val selectedCalendar = Calendar.getInstance()
        etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time))

        etDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedCalendar.set(year, month, day)
                    etDate.setText("$day/${month + 1}/$year")
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnAdd.setOnClickListener {
            try {
                val category = spinnerType.selectedItem?.toString() ?: ""
                val amountStr = etAmount.text.toString().trim()
                val note = etNote.text.toString().trim()

                android.util.Log.d("DashboardDebug", "=== ADD INCOME CLICKED ===")
                android.util.Log.d("DashboardDebug", "Category: $category")
                android.util.Log.d("DashboardDebug", "Amount: $amountStr")
                android.util.Log.d("DashboardDebug", "Note: $note")
                android.util.Log.d("DashboardDebug", "UserId: $userId")

                if (category.isEmpty()) {
                    Toast.makeText(requireContext(), "Pilih kategori!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = amountStr.toLongOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (userId == null) {
                    Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val transaction = hashMapOf(
                    "user_id" to userId!!,
                    "type" to "income",
                    "category" to category,
                    "amount" to amount,
                    "date" to Timestamp(selectedCalendar.time),
                    "created_at" to Timestamp(Date()),
                    "note" to note
                )

                android.util.Log.d("DashboardDebug", "Saving to Firestore: $transaction")

                firestore.collection("Transactions")
                    .add(transaction)
                    .addOnSuccessListener { documentReference ->
                        android.util.Log.d("DashboardDebug", "✅ Income saved! Doc ID: ${documentReference.id}")
                        Toast.makeText(requireContext(), "Pemasukan ditambahkan!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadTransactions(userId!!)
                        loadSummary(userId!!)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("DashboardDebug", "❌ Failed to save income: ${e.message}", e)
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                android.util.Log.e("DashboardDebug", "Error in income dialog: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }

    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambahkan Pengeluaran")
            .setView(dialogView)
            .create()

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddExpense)

        val types = listOf("Makan", "Transport", "Belanja", "Hiburan", "Lainnya")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter

        val selectedCalendar = Calendar.getInstance()
        etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time))

        etDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedCalendar.set(year, month, day)
                    etDate.setText("$day/${month + 1}/$year")
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnAdd.setOnClickListener {
            try {
                val category = spinnerType.selectedItem?.toString() ?: ""
                val amountStr = etAmount.text.toString().trim()
                val note = etNote.text.toString().trim()

                if (category.isEmpty()) {
                    Toast.makeText(requireContext(), "Pilih kategori!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = amountStr.toLongOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (userId == null) {
                    Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Get current location before saving
                getCurrentLocationAndSaveExpense(category, amount, note, selectedCalendar, dialog)

            } catch (e: Exception) {
                android.util.Log.e("DashboardDebug", "Error in expense dialog: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }

    private fun getCurrentLocationAndSaveExpense(
        category: String,
        amount: Long,
        note: String,
        selectedCalendar: Calendar,
        dialog: AlertDialog
    ) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // No permission, save without location
            android.util.Log.d("DashboardDebug", "No location permission, saving without location")
            saveExpenseTransaction(category, amount, note, selectedCalendar, null, null, dialog)
            return
        }

        // Show loading toast
        Toast.makeText(requireContext(), "Mengambil lokasi...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                android.util.Log.d("DashboardDebug", "Got location: ${location.latitude}, ${location.longitude}")
                saveExpenseTransaction(
                    category,
                    amount,
                    note,
                    selectedCalendar,
                    location.latitude,
                    location.longitude,
                    dialog
                )
            } else {
                // Location null, try to request fresh location
                android.util.Log.d("DashboardDebug", "LastLocation is null, requesting fresh location...")

                requestFreshLocation { freshLocation ->
                    if (freshLocation != null) {
                        android.util.Log.d("DashboardDebug", "Got fresh location: ${freshLocation.latitude}, ${freshLocation.longitude}")
                        saveExpenseTransaction(
                            category,
                            amount,
                            note,
                            selectedCalendar,
                            freshLocation.latitude,
                            freshLocation.longitude,
                            dialog
                        )
                    } else {
                        android.util.Log.d("DashboardDebug", "Fresh location also null, saving without location")
                        Toast.makeText(requireContext(), "Tidak dapat lokasi, menyimpan tanpa lokasi", Toast.LENGTH_SHORT).show()
                        saveExpenseTransaction(category, amount, note, selectedCalendar, null, null, dialog)
                    }
                }
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("DashboardDebug", "Failed to get location: ${e.message}")
            Toast.makeText(requireContext(), "Gagal mendapatkan lokasi, menyimpan tanpa lokasi", Toast.LENGTH_SHORT).show()
            saveExpenseTransaction(category, amount, note, selectedCalendar, null, null, dialog)
        }
    }

    private fun requestFreshLocation(onResult: (android.location.Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null)
            return
        }

        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
            maxWaitTime = 5000 // 5 seconds timeout
        }

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                val location = locationResult.lastLocation
                fusedLocationClient.removeLocationUpdates(this)
                onResult(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: Exception) {
            android.util.Log.e("DashboardDebug", "Error requesting location updates: ${e.message}")
            onResult(null)
        }
    }

    private fun saveExpenseTransaction(
        category: String,
        amount: Long,
        note: String,
        selectedCalendar: Calendar,
        latitude: Double?,
        longitude: Double?,
        dialog: AlertDialog
    ) {
        val transaction = hashMapOf(
            "user_id" to userId!!,
            "type" to "expense",
            "category" to category,
            "amount" to amount,
            "date" to Timestamp(selectedCalendar.time),
            "created_at" to Timestamp(Date()),
            "note" to note
        )

        // Add location if available
        if (latitude != null && longitude != null) {
            transaction["latitude"] = latitude
            transaction["longitude"] = longitude
            android.util.Log.d("DashboardDebug", "Saving with location: $latitude, $longitude")
        } else {
            android.util.Log.d("DashboardDebug", "Saving without location")
        }

        firestore.collection("Transactions")
            .add(transaction)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengeluaran ditambahkan!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadTransactions(userId!!)
                loadSummary(userId!!)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DashboardDebug", "Failed to save expense: ${e.message}", e)
                Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showAddSavingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_saving, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambahkan Tabungan")
            .setView(dialogView)
            .create()

        val spinnerGoal = dialogView.findViewById<Spinner>(R.id.spinnerGoal)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddSaving)

        val goalNames = mutableListOf<String>()
        val goalIds = mutableListOf<String>()

        if (userId != null) {
            // Show loading
            goalNames.add("Memuat tujuan...")
            val tempAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goalNames)
            spinnerGoal.adapter = tempAdapter
            btnAdd.isEnabled = false

            firestore.collection("Goals")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener { documents ->
                    goalNames.clear()
                    goalIds.clear()

                    for (doc in documents) {
                        val goalName = doc.getString("goal_name") ?: "Goal"
                        val currentAmount = doc.getLong("current_amount") ?: 0
                        val targetAmount = doc.getLong("target_amount") ?: 0
                        val progress = if (targetAmount > 0) {
                            ((currentAmount.toDouble() / targetAmount) * 100).toInt()
                        } else 0

                        goalNames.add("$goalName ($progress%)")
                        goalIds.add(doc.id)
                    }

                    if (goalNames.isEmpty()) {
                        goalNames.add("⚠️ Belum ada tujuan tabungan")
                        btnAdd.isEnabled = false
                        Toast.makeText(requireContext(), "Buat tujuan tabungan dulu di menu Target Tabungan!", Toast.LENGTH_LONG).show()
                    } else {
                        btnAdd.isEnabled = true
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goalNames)
                    spinnerGoal.adapter = adapter
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardDebug", "Failed to load goals: ${e.message}", e)
                    goalNames.clear()
                    goalNames.add("❌ Gagal memuat tujuan")
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goalNames)
                    spinnerGoal.adapter = adapter
                    btnAdd.isEnabled = false
                    Toast.makeText(requireContext(), "Gagal memuat tujuan tabungan", Toast.LENGTH_SHORT).show()
                }
        }

        val selectedCalendar = Calendar.getInstance()
        etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time))

        etDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedCalendar.set(year, month, day)
                    etDate.setText("$day/${month + 1}/$year")
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnAdd.setOnClickListener {
            try {
                if (goalIds.isEmpty()) {
                    Toast.makeText(requireContext(), "Buat tujuan tabungan dulu!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedIndex = spinnerGoal.selectedItemPosition
                val amountStr = etAmount.text.toString().trim()
                val note = etNote.text.toString().trim()

                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = amountStr.toLongOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (userId == null) {
                    Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedGoalId = goalIds[selectedIndex]
                val selectedGoalName = goalNames[selectedIndex].substringBefore(" (")

                val transaction = hashMapOf(
                    "user_id" to userId!!,
                    "type" to "saving",
                    "goal_id" to selectedGoalId,
                    "category" to selectedGoalName,
                    "amount" to amount,
                    "date" to Timestamp(selectedCalendar.time),
                    "created_at" to Timestamp(Date()),
                    "note" to if (note.isNotEmpty()) note else "Tabungan untuk $selectedGoalName"
                )

                android.util.Log.d("DashboardDebug", "Saving transaction: $transaction")

                firestore.collection("Transactions")
                    .add(transaction)
                    .addOnSuccessListener {
                        // Update goal's current_amount
                        val goalRef = firestore.collection("Goals").document(selectedGoalId)
                        firestore.runTransaction { trans ->
                            val goalSnapshot = trans.get(goalRef)
                            val currentAmount = goalSnapshot.getLong("current_amount") ?: 0L
                            trans.update(goalRef, "current_amount", currentAmount + amount)
                        }.addOnSuccessListener {
                            Toast.makeText(requireContext(), "Tabungan ditambahkan ke $selectedGoalName!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadTransactions(userId!!)
                            loadSummary(userId!!)
                        }.addOnFailureListener { e ->
                            android.util.Log.e("DashboardDebug", "Failed to update goal: ${e.message}", e)
                            Toast.makeText(requireContext(), "Transaksi tersimpan tapi gagal update goal", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadTransactions(userId!!)
                            loadSummary(userId!!)
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("DashboardDebug", "Failed to save saving: ${e.message}", e)
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                android.util.Log.e("DashboardDebug", "Error in saving dialog: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }

    private fun checkAndAddRecurringIncome(uid: String) {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        android.util.Log.d("RecurringIncome", "Checking recurring income for day: $currentDay")

        // Get active recurring income
        firestore.collection("RecurringIncome")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("is_active", true)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    android.util.Log.d("RecurringIncome", "No active recurring income found")
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val dayOfMonth = doc.getLong("day_of_month")?.toInt() ?: continue
                    val amount = doc.getLong("amount") ?: continue
                    val jobTitle = doc.getString("job_title") ?: "Gaji"
                    val recurringIncomeId = doc.id

                    android.util.Log.d("RecurringIncome", "Found: $jobTitle, day=$dayOfMonth, amount=$amount")

                    // Check if today is payday
                    if (currentDay == dayOfMonth) {
                        android.util.Log.d("RecurringIncome", "✅ Today is payday!")

                        // Check if already added this month
                        val monthStart = Calendar.getInstance().apply {
                            set(currentYear, currentMonth, 1, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val monthEnd = Calendar.getInstance().apply {
                            set(currentYear, currentMonth + 1, 1, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        firestore.collection("Transactions")
                            .whereEqualTo("user_id", uid)
                            .whereEqualTo("recurring_income_id", recurringIncomeId)
                            .whereGreaterThanOrEqualTo("date", Timestamp(monthStart.time))
                            .whereLessThan("date", Timestamp(monthEnd.time))
                            .get()
                            .addOnSuccessListener { transactions ->
                                if (transactions.isEmpty) {
                                    android.util.Log.d("RecurringIncome", "Creating auto transaction...")
                                    addRecurringIncomeTransaction(uid, recurringIncomeId, jobTitle, amount, today)
                                } else {
                                    android.util.Log.d("RecurringIncome", "Transaction already exists for this month")
                                }
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("RecurringIncome", "Error checking transactions: ${e.message}")
                            }
                    } else {
                        android.util.Log.d("RecurringIncome", "Not payday yet (current=$currentDay, payday=$dayOfMonth)")
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("RecurringIncome", "Error loading recurring income: ${e.message}")
            }
    }

    private fun addRecurringIncomeTransaction(
        uid: String,
        recurringIncomeId: String,
        jobTitle: String,
        amount: Long,
        date: Calendar
    ) {
        val transaction = hashMapOf(
            "user_id" to uid,
            "type" to "income",
            "category" to jobTitle,
            "amount" to amount,
            "date" to Timestamp(date.time),
            "created_at" to Timestamp(Date()),
            "note" to "Gaji bulanan - $jobTitle",
            "recurring_income_id" to recurringIncomeId
        )

        android.util.Log.d("RecurringIncome", "Adding transaction: $transaction")

        firestore.collection("Transactions")
            .add(transaction)
            .addOnSuccessListener { docRef ->
                android.util.Log.d("RecurringIncome", "✅ Auto transaction created: ${docRef.id}")
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formatted = formatter.format(amount).replace("Rp", "Rp ")

                Toast.makeText(
                    requireContext(),
                    "🎉 Gaji bulanan $formatted otomatis ditambahkan!",
                    Toast.LENGTH_LONG
                ).show()

                // Refresh data
                loadTransactions(uid)
                loadSummary(uid)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("RecurringIncome", "❌ Failed to add auto transaction: ${e.message}")
            }
    }
}
