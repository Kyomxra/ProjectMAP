package com.example.projectmap

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var tvUserId: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDob: TextView

    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().getReference("profile_pictures")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        imgProfile = view.findViewById(R.id.imgProfile)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        tvUserId = view.findViewById(R.id.tvUserId)
        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvDob = view.findViewById(R.id.tvDob)

        loadUserProfile()

        btnChangePhoto.setOnClickListener {
            pickImageFromGallery()
        }

        return view
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid
        tvUserId.text = "User ID: $userId"

        firestore.collection("User").document(user.uid)   // ✅ now matches your Firestore doc ID
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fName = document.getString("FName") ?: ""
                    val lName = document.getString("LName") ?: ""
                    val email = document.getString("Email") ?: ""
                    val dob = document.get("DOB") // bisa Timestamp atau String
                    val imageUrl = document.getString("imageURL")

                    tvDisplayName.text = "Nama: $fName $lName"
                    tvEmail.text = "Email: $email"

                    // tampilkan DOB
                    if (dob is com.google.firebase.Timestamp) {
                        val date = dob.toDate()
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        tvDob.text = "DOB: ${sdf.format(date)}"
                    } else if (dob is String) {
                        tvDob.text = "DOB: $dob"
                    }

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext()).load(imageUrl).into(imgProfile)
                    }
                } else {
                    Toast.makeText(requireContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            uploadImageToFirebase()
        }
    }

    private fun uploadImageToFirebase() {
        val user = auth.currentUser ?: return
        val fileRef = storageRef.child("${user.uid}.jpg")

        imageUri?.let { uri ->
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        firestore.collection("users").document(user.uid)
                            .update("imageURL", downloadUri.toString())
                            .addOnSuccessListener {
                                Glide.with(requireContext()).load(downloadUri).into(imgProfile)
                                Toast.makeText(requireContext(), "Foto profil diperbarui!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal upload foto", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
