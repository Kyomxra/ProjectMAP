package com.example.projectmap

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReportFragment : Fragment(R.layout.fragment_report) {

    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private var userBudget: Long = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarReport)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        userId = prefs.getString("userId", null)

        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val tvTotalExpense = view.findViewById<TextView>(R.id.tvTotalExpense)
        val tvBudgetAmount = view.findViewById<TextView>(R.id.tvBudgetAmount)
        val tvComparison = view.findViewById<TextView>(R.id.tvComparison)
        val tvComparisonDetail = view.findViewById<TextView>(R.id.tvComparisonDetail)
        val tvRecommendation = view.findViewById<TextView>(R.id.tvRecommendation)
        val btnSetBudget = view.findViewById<Button>(R.id.btnSetBudget)

        // Set bulan dan tahun saat ini
        val calendar = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)
        tvMonthYear.text = "Laporan Bulan $monthName"

        if (userId == null) {
            tvTotalExpense.text = "Rp 0"
            tvBudgetAmount.text = "Rp 0"
            tvComparison.text = "Belum login"
            tvComparisonDetail.text = "Silakan login untuk melihat laporan"
            Toast.makeText(requireContext(), "Belum login!", Toast.LENGTH_SHORT).show()
            return
        }

        // Load budget dari Firestore
        loadUserBudget(userId!!, tvBudgetAmount)

        // Load expense data
        loadExpenseData(userId!!, tvTotalExpense, tvComparison, tvComparisonDetail, tvRecommendation)

        // Button untuk set/edit budget
        btnSetBudget.setOnClickListener {
            showSetBudgetDialog(tvBudgetAmount)
        }
    }

    private fun loadUserBudget(uid: String, tvBudgetAmount: TextView) {
        firestore.collection("User").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userBudget = document.getLong("monthly_budget") ?: 0L

                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    if (userBudget > 0) {
                        tvBudgetAmount.text = formatter.format(userBudget).replace("Rp", "Rp ")
                    } else {
                        tvBudgetAmount.text = "Belum diatur"
                    }

                    android.util.Log.d("ReportFragment", "Budget loaded: $userBudget")
                } else {
                    tvBudgetAmount.text = "Belum diatur"
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportFragment", "Error loading budget: ${e.message}")
                tvBudgetAmount.text = "Belum diatur"
            }
    }

    private fun showSetBudgetDialog(tvBudgetAmount: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)

        // Pre-fill dengan budget yang ada jika sudah diset
        if (userBudget > 0) {
            etBudget.setText(userBudget.toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Atur Budget Bulanan")
            .setMessage("Masukkan target budget pengeluaran bulananmu")
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val budgetStr = etBudget.text.toString().trim()

                if (budgetStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Masukkan jumlah budget!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val budget = budgetStr.toLongOrNull()
                if (budget == null || budget <= 0) {
                    Toast.makeText(requireContext(), "Budget harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveBudgetToFirestore(budget, tvBudgetAmount)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveBudgetToFirestore(budget: Long, tvBudgetAmount: TextView) {
        if (userId == null) return

        firestore.collection("User").document(userId!!)
            .update("monthly_budget", budget)
            .addOnSuccessListener {
                userBudget = budget
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvBudgetAmount.text = formatter.format(budget).replace("Rp", "Rp ")
                Toast.makeText(requireContext(), "Budget berhasil disimpan!", Toast.LENGTH_SHORT).show()

                android.util.Log.d("ReportFragment", "Budget saved: $budget")

                // Refresh comparison
                val tvTotalExpense = view?.findViewById<TextView>(R.id.tvTotalExpense)
                val tvComparison = view?.findViewById<TextView>(R.id.tvComparison)
                val tvComparisonDetail = view?.findViewById<TextView>(R.id.tvComparisonDetail)
                val tvRecommendation = view?.findViewById<TextView>(R.id.tvRecommendation)

                if (tvTotalExpense != null && tvComparison != null && tvComparisonDetail != null && tvRecommendation != null) {
                    loadExpenseData(userId!!, tvTotalExpense, tvComparison, tvComparisonDetail, tvRecommendation)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportFragment", "Error saving budget: ${e.message}")
                Toast.makeText(requireContext(), "Gagal menyimpan budget: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadExpenseData(
        uid: String,
        tvTotalExpense: TextView,
        tvComparison: TextView,
        tvComparisonDetail: TextView,
        tvRecommendation: TextView
    ) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Start of month
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = Timestamp(calendar.time)

        // End of month
        calendar.set(currentYear, currentMonth + 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val endOfMonth = Timestamp(calendar.time)

        android.util.Log.d("ReportFragment", "Loading expenses for userId: $uid")
        android.util.Log.d("ReportFragment", "Period: $startOfMonth to $endOfMonth")

        // Query tanpa compound index - filter di client side
        firestore.collection("Transactions")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("type", "expense")
            .get()
            .addOnSuccessListener { documents ->
                var totalExpense = 0L

                for (doc in documents) {
                    val timestamp = doc.getTimestamp("date")
                    if (timestamp != null) {
                        // Filter bulan ini di client side
                        if (timestamp.toDate().after(startOfMonth.toDate()) &&
                            timestamp.toDate().before(endOfMonth.toDate())) {
                            val amount = doc.getLong("amount") ?: 0
                            totalExpense += amount
                            android.util.Log.d("ReportFragment", "Adding expense: $amount")
                        }
                    }
                }

                android.util.Log.d("ReportFragment", "Total expense this month: $totalExpense")

                // Format currency
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvTotalExpense.text = formatter.format(totalExpense).replace("Rp", "Rp ")

                // Calculate comparison
                calculateComparison(totalExpense, tvComparison, tvComparisonDetail, tvRecommendation)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportFragment", "Error loading expenses: ${e.message}", e)
                tvTotalExpense.text = "Rp 0"
                tvComparison.text = "Error"
                tvComparisonDetail.text = "Gagal memuat data: ${e.message}"
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun calculateComparison(
        userExpense: Long,
        tvComparison: TextView,
        tvComparisonDetail: TextView,
        tvRecommendation: TextView
    ) {
        if (userBudget == 0L) {
            tvComparison.text = "⚙️ Budget belum diatur"
            tvComparison.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            tvComparisonDetail.text = "Atur budget bulananmu terlebih dahulu untuk melihat perbandingan"
            tvRecommendation.text = "💡 Klik tombol 'Atur Budget' di atas untuk mulai mengelola keuanganmu!"
            return
        }

        when {
            userExpense == 0L -> {
                tvComparison.text = "Belum ada pengeluaran"
                tvComparison.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                tvComparisonDetail.text = "Kamu belum mencatat pengeluaran bulan ini"
                tvRecommendation.text = "💡 Mulai catat pengeluaranmu untuk manajemen keuangan yang lebih baik!"
            }
            userExpense < userBudget -> {
                val remaining = userBudget - userExpense
                val usedPercentage = ((userExpense.toDouble() / userBudget) * 100).toInt()

                tvComparison.text = "🎉 Kamu masih di jalur yang benar!"
                tvComparison.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formattedRemaining = formatter.format(remaining).replace("Rp", "Rp ")

                tvComparisonDetail.text = "Kamu sudah menggunakan $usedPercentage% dari budget. Sisa budget: $formattedRemaining"
                tvRecommendation.text = "💰 Bagus! Pertahankan kebiasaan ini hingga akhir bulan. Sisihkan sisanya untuk tabungan!"
            }
            userExpense == userBudget -> {
                tvComparison.text = "👌 Budget habis tepat!"
                tvComparison.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                tvComparisonDetail.text = "Pengeluaranmu sama persis dengan budget yang diatur"
                tvRecommendation.text = "📊 Coba sisihkan sebagian untuk dana darurat atau tabungan!"
            }
            else -> {
                val overbudget = userExpense - userBudget
                val overPercentage = (((userExpense - userBudget).toDouble() / userBudget) * 100).toInt()

                tvComparison.text = "⚠️ Pengeluaran melebihi budget"
                tvComparison.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formattedOver = formatter.format(overbudget).replace("Rp", "Rp ")

                tvComparisonDetail.text = "Kamu sudah over budget $formattedOver ($overPercentage% lebih tinggi dari target)"
                tvRecommendation.text = "💡 Evaluasi pengeluaranmu! Kurangi pengeluaran tidak penting dan pertimbangkan untuk menyesuaikan budget bulan depan."
            }
        }
    }
}