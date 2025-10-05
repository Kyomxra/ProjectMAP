package com.example.projectmap

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGetStarted = view.findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            showLoginPopup()
        }
    }

    private fun showLoginPopup() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.popup_login)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val emailInput = dialog.findViewById<EditText>(R.id.etEmail)
        val passwordInput = dialog.findViewById<EditText>(R.id.etPassword)
        val btnMasuk = dialog.findViewById<Button>(R.id.btnMasuk)

        btnMasuk.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                checkLogin(email, password, dialog)
            } else {
                Toast.makeText(requireContext(), "Isi semua data", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()

        // atur ukuran popup
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // efek dim
        dialog.window?.let { window ->
            val lp = window.attributes
            lp.dimAmount = 0.5f
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = lp
        }
    }

    private fun checkLogin(email: String, password: String, dialog: Dialog) {
        db.collection("User")
            .get()
            .addOnSuccessListener { result ->
                var isValidUser = false
                var userId: String? = null

                for (document in result) {
                    val dbEmail = document.getString("Email")
                    val dbPassword = document.getString("Password")

                    if (email == dbEmail && password == dbPassword) {
                        isValidUser = true
                        userId = document.id  // 🔹 simpan documentId sebagai userId
                        break
                    }
                }

                if (isValidUser && userId != null) {
                    // 🔹 simpan userId ke SharedPreferences
                    val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
                    prefs.edit().putString("userId", userId).apply()

                    Toast.makeText(requireContext(), "Login berhasil!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                    // 🔹 Pindah ke Dashboard
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, DashboardFragment())
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "Email atau password salah", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
