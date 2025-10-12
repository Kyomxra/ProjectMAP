package com.example.projectmap

import android.os.Bundle
import android.view.View
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

    // Statistik rata-rata pengeluaran mahasiswa per bulan (dalam Rupiah)
    private val averageStudentExpense = 1_500_000L // Rp 1.5 juta

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
        val tvAverageExpense = view.findViewById<TextView>(R.id.tvAverageExpense)
        val tvComparison = view.findViewById<TextView>(R.id.tvComparison)
        val tvComparisonDetail = view.findViewById<TextView>(R.id.tvComparisonDetail)
        val tvRecommendation = view.findViewById<TextView>(R.id.tvRecommendation)

        // Set bulan dan tahun saat ini
        val calendar = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)
        tvMonthYear.text = "Laporan Bulan $monthName"

        // Set rata-rata pengeluaran mahasiswa
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        tvAverageExpense.text = formatter.format(averageStudentExpense).replace("Rp", "Rp ")

        if (userId == null) {
            tvTotalExpense.text = "Rp 0"
            tvComparison.text = "Belum login"
            tvComparisonDetail.text = "Silakan login untuk melihat laporan"
            Toast.makeText(requireContext(), "Belum login!", Toast.LENGTH_SHORT).show()
            return
        }

        loadExpenseData(userId!!, tvTotalExpense, tvComparison, tvComparisonDetail, tvRecommendation)
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
        when {
            userExpense == 0L -> {
                tvComparison.text = "Belum ada pengeluaran"
                tvComparison.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                tvComparisonDetail.text = "Kamu belum mencatat pengeluaran bulan ini"
                tvRecommendation.text = "💡 Mulai catat pengeluaranmu untuk manajemen keuangan yang lebih baik!"
            }
            userExpense < averageStudentExpense -> {
                val difference = averageStudentExpense - userExpense
                val percentage = ((averageStudentExpense - userExpense).toDouble() / averageStudentExpense * 100).toInt()

                tvComparison.text = "🎉 Pengeluaranmu lebih hemat!"
                tvComparison.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formattedDiff = formatter.format(difference).replace("Rp", "Rp ")

                tvComparisonDetail.text = "Kamu menghemat $formattedDiff ($percentage%) dibanding rata-rata mahasiswa"
                tvRecommendation.text = "💰 Bagus! Teruskan kebiasaan berhemat ini. Pertimbangkan untuk menabung kelebihan budget-mu!"
            }
            userExpense == averageStudentExpense -> {
                tvComparison.text = "👌 Pengeluaranmu pas rata-rata"
                tvComparison.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                tvComparisonDetail.text = "Pengeluaranmu sama dengan rata-rata mahasiswa pada umumnya"
                tvRecommendation.text = "📊 Coba identifikasi pos pengeluaran yang bisa dikurangi untuk lebih hemat!"
            }
            else -> {
                val difference = userExpense - averageStudentExpense
                val percentage = ((userExpense - averageStudentExpense).toDouble() / averageStudentExpense * 100).toInt()

                tvComparison.text = "⚠️ Pengeluaranmu lebih tinggi"
                tvComparison.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formattedDiff = formatter.format(difference).replace("Rp", "Rp ")

                tvComparisonDetail.text = "Pengeluaranmu melebihi $formattedDiff ($percentage%) dari rata-rata mahasiswa"
                tvRecommendation.text = "💡 Coba evaluasi pengeluaranmu! Kurangi pengeluaran tidak penting dan buat budget yang lebih ketat."
            }
        }
    }
}