package com.example.projectmap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class LoginFragment : Fragment(R.layout.fragment_login) {

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
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Toast.makeText(requireContext(), "Login berhasil!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // 🔹 Ganti fragment ke DashboardFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DashboardFragment())
                    .commit()
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
}
