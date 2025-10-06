package com.example.projectmap

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etUserId = view.findViewById<EditText>(R.id.etUserId)
        val etFName = view.findViewById<EditText>(R.id.etFName)
        val etLName = view.findViewById<EditText>(R.id.etLName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }


        btnRegister.setOnClickListener {
            val userId = etUserId.text.toString().trim()
            val fName = etFName.text.toString().trim()
            val lName = etLName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (userId.isEmpty() || fName.isEmpty() || lName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Isi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Password tidak sama", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cek apakah user_id sudah dipakai
            db.collection("User").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Toast.makeText(requireContext(), "User ID sudah dipakai!", Toast.LENGTH_SHORT).show()
                    } else {
                        // 🔹 data user sesuai struktur Firestore kamu
                        val user = hashMapOf(
                            "user_id" to userId,
                            "FName" to fName,
                            "LName" to lName,
                            "Email" to email,
                            "Password" to password,
                            "DOB" to "",  // default kosong
                            "imageURL" to "" // default kosong
                        )

                        // simpan dengan userId sebagai documentId
                        db.collection("User").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Registrasi berhasil!", Toast.LENGTH_SHORT).show()

                                // simpan userId ke SharedPreferences
                                val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
                                prefs.edit().putString("userId", userId).apply()

                                // balik ke login popup
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
