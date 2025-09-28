package com.example.projectmap

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Dialog
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.LinearLayout


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            showLoginPopup()
        }
    }

    private fun showLoginPopup() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_login)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val emailInput = dialog.findViewById<EditText>(R.id.etEmail)
        val passwordInput = dialog.findViewById<EditText>(R.id.etPassword)
        val btnMasuk = dialog.findViewById<Button>(R.id.btnMasuk)

        btnMasuk.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Isi semua data", Toast.LENGTH_SHORT).show()
            }
        }

        // show dulu biar window sudah attach
        dialog.show()

        // Atur ukuran popup
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Tambahin efek dim
        dialog.window?.let { window ->
            val lp = window.attributes
            lp.dimAmount = 0.5f // 0.0 = no dim, 1.0 = full black
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = lp
        }
    }

}