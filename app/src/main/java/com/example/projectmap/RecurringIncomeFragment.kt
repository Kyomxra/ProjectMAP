package com.example.projectmap

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.*

class RecurringIncomeFragment : Fragment(R.layout.fragment_recurring_income) {

    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private var recurringIncomeId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        toolbar?.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        userId = prefs.getString("userId", null)

        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val etJobTitle = view.findViewById<EditText>(R.id.etJobTitle)
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val spinnerDay = view.findViewById<Spinner>(R.id.spinnerDay)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        val days = (1..31).map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        spinnerDay.adapter = adapter

        if (userId == null) {
            Toast.makeText(requireContext(), "Belum login!", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        loadRecurringIncome(tvStatus, etJobTitle, etAmount, spinnerDay, btnDelete)

        btnSave.setOnClickListener {
            saveRecurringIncome(etJobTitle, etAmount, spinnerDay, tvStatus, btnDelete)
        }

        btnDelete.setOnClickListener {
            deleteRecurringIncome(tvStatus, etJobTitle, etAmount, spinnerDay, btnDelete)
        }
    }

    private fun loadRecurringIncome(
        tvStatus: TextView,
        etJobTitle: EditText,
        etAmount: EditText,
        spinnerDay: Spinner,
        btnDelete: Button
    ) {
        firestore.collection("RecurringIncome")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_active", true)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    recurringIncomeId = doc.id

                    val jobTitle = doc.getString("job_title") ?: ""
                    val amount = doc.getLong("amount") ?: 0
                    val dayOfMonth = doc.getLong("day_of_month")?.toInt() ?: 25

                    tvStatus.text = "✓ Sudah ada pendapatan pokok aktif"
                    tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    etJobTitle.setText(jobTitle)
                    etAmount.setText(amount.toString())
                    spinnerDay.setSelection(dayOfMonth - 1)
                    btnDelete.visibility = View.VISIBLE
                } else {
                    tvStatus.text = "Belum ada pendapatan pokok"
                    tvStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    btnDelete.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memuat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveRecurringIncome(
        etJobTitle: EditText,
        etAmount: EditText,
        spinnerDay: Spinner,
        tvStatus: TextView,
        btnDelete: Button
    ) {
        val jobTitle = etJobTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val dayOfMonth = spinnerDay.selectedItem.toString().toInt()

        android.util.Log.d("RecurringIncome", "=== SAVING ===")
        android.util.Log.d("RecurringIncome", "Job: $jobTitle, Amount: $amountStr, Day: $dayOfMonth")

        if (jobTitle.isEmpty()) {
            Toast.makeText(requireContext(), "Masukkan nama pekerjaan!", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Masukkan gaji bulanan!", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toLongOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Gaji harus lebih dari 0!", Toast.LENGTH_SHORT).show()
            return
        }

        val now = Timestamp.now()
        val data = hashMapOf(
            "user_id" to userId!!,
            "job_title" to jobTitle,
            "amount" to amount,
            "day_of_month" to dayOfMonth,
            "is_active" to true,
            "created_at" to now,
            "updated_at" to now
        )

        if (recurringIncomeId != null) {
            // Update
            firestore.collection("RecurringIncome")
                .document(recurringIncomeId!!)
                .update(data as Map<String, Any>)
                .addOnSuccessListener {
                    android.util.Log.d("RecurringIncome", "✅ Updated")
                    Toast.makeText(requireContext(), "Pendapatan pokok diperbarui!", Toast.LENGTH_SHORT).show()
                    checkAndAddTodayIncome(recurringIncomeId!!, jobTitle, amount, dayOfMonth)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Create
            firestore.collection("RecurringIncome")
                .add(data)
                .addOnSuccessListener { doc ->
                    recurringIncomeId = doc.id
                    android.util.Log.d("RecurringIncome", "✅ Created: ${doc.id}")
                    Toast.makeText(requireContext(), "Pendapatan pokok berhasil disimpan!", Toast.LENGTH_SHORT).show()

                    tvStatus.text = "✓ Sudah ada pendapatan pokok aktif"
                    tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    btnDelete.visibility = View.VISIBLE

                    checkAndAddTodayIncome(doc.id, jobTitle, amount, dayOfMonth)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkAndAddTodayIncome(
        recurringIncomeId: String,
        jobTitle: String,
        amount: Long,
        dayOfMonth: Int
    ) {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        android.util.Log.d("RecurringIncome", "=== CHECKING TODAY ===")
        android.util.Log.d("RecurringIncome", "Current day: $currentDay, Payday: $dayOfMonth")

        if (currentDay != dayOfMonth) {
            android.util.Log.d("RecurringIncome", "❌ Not payday yet")
            Toast.makeText(
                requireContext(),
                "Pendapatan akan otomatis ditambahkan setiap tanggal $dayOfMonth",
                Toast.LENGTH_LONG
            ).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                parentFragmentManager.popBackStack()
            }, 1500)
            return
        }

        android.util.Log.d("RecurringIncome", "✅ Today IS payday!")

        // Check existing transaction this month
        val monthStart = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthEnd = Calendar.getInstance().apply {
            set(currentYear, currentMonth + 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        android.util.Log.d("RecurringIncome", "Checking existing transactions...")

        firestore.collection("Transactions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("recurring_income_id", recurringIncomeId)
            .whereGreaterThanOrEqualTo("date", Timestamp(monthStart.time))
            .whereLessThan("date", Timestamp(monthEnd.time))
            .get()
            .addOnSuccessListener { docs ->
                android.util.Log.d("RecurringIncome", "Found ${docs.size()} existing transactions")

                if (docs.isEmpty) {
                    android.util.Log.d("RecurringIncome", "No transaction found, creating...")
                    createTransaction(recurringIncomeId, jobTitle, amount)
                } else {
                    android.util.Log.d("RecurringIncome", "Transaction already exists")
                    Toast.makeText(
                        requireContext(),
                        "Gaji bulan ini sudah ditambahkan",
                        Toast.LENGTH_LONG
                    ).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        parentFragmentManager.popBackStack()
                    }, 1500)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("RecurringIncome", "Error: ${e.message}")
                // Still try to create transaction
                createTransaction(recurringIncomeId, jobTitle, amount)
            }
    }

    private fun createTransaction(
        recurringIncomeId: String,
        jobTitle: String,
        amount: Long
    ) {
        val now = Calendar.getInstance()
        val transaction = hashMapOf(
            "user_id" to userId!!,
            "type" to "income",
            "category" to jobTitle,
            "amount" to amount,
            "date" to Timestamp(now.time),
            "created_at" to Timestamp.now(),
            "note" to "Gaji bulanan - $jobTitle",
            "recurring_income_id" to recurringIncomeId
        )

        android.util.Log.d("RecurringIncome", "Creating transaction: $transaction")

        firestore.collection("Transactions")
            .add(transaction)
            .addOnSuccessListener { docRef ->
                android.util.Log.d("RecurringIncome", "✅✅✅ TRANSACTION CREATED: ${docRef.id}")

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formatted = formatter.format(amount).replace("Rp", "Rp ")

                Toast.makeText(
                    requireContext(),
                    "🎉 Gaji $formatted berhasil ditambahkan!",
                    Toast.LENGTH_LONG
                ).show()

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    parentFragmentManager.popBackStack()
                }, 1500)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("RecurringIncome", "❌ FAILED: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Gagal menambahkan transaksi: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun deleteRecurringIncome(
        tvStatus: TextView,
        etJobTitle: EditText,
        etAmount: EditText,
        spinnerDay: Spinner,
        btnDelete: Button
    ) {
        if (recurringIncomeId == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pendapatan Pokok")
            .setMessage("Yakin hapus? Transaksi yang sudah ada tidak akan terhapus.")
            .setPositiveButton("Hapus") { _, _ ->
                firestore.collection("RecurringIncome")
                    .document(recurringIncomeId!!)
                    .update("is_active", false, "updated_at", Timestamp.now())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Pendapatan pokok dihapus!", Toast.LENGTH_SHORT).show()

                        recurringIncomeId = null
                        tvStatus.text = "Belum ada pendapatan pokok"
                        tvStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                        etJobTitle.setText("")
                        etAmount.setText("")
                        spinnerDay.setSelection(24)
                        btnDelete.visibility = View.GONE
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}