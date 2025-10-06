package com.example.projectmap

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Spinner
import java.util.Calendar


class ProfileFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var tvUserId: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDob: TextView

    private lateinit var btnEditDob: Button


    private val PICK_IMAGE_REQUEST = 1001
    private val CAMERA_REQUEST = 1002
    private var imageUri: Uri? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().getReference("profile_pictures")

    private var userId: String? = null
    private var cameraPhotoUri: Uri? = null

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
        btnEditDob = view.findViewById(R.id.btnEditDob)


        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        userId = prefs.getString("userId", null)

        if (userId != null) {
            loadUserProfile(userId!!)
        } else {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
        }

        btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        btnEditDob.setOnClickListener {
            showDatePickerDialog()
        }


        return view
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery", "Camera")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> captureImageFromCamera()
                }
            }
            .show()
    }

    private fun showDatePickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_dob_picker, null)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinnerDay)
        val spinnerMonth = dialogView.findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerYear = dialogView.findViewById<Spinner>(R.id.spinnerYear)

        // Daftar tanggal 1–31
        val days = (1..31).map { it.toString() }

        // Daftar bulan
        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        // 🔹 Ini dia bagian years (tahun 1950 sampai tahun sekarang)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (1950..currentYear).map { it.toString() }.reversed()
        // `reversed()` biar tahun terbaru muncul di atas dropdown

        // Isi semua spinner
        spinnerDay.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        spinnerMonth.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spinnerYear.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

        // Bangun dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val selectedDay = spinnerDay.selectedItem.toString().padStart(2, '0')
                val selectedMonth = spinnerMonth.selectedItem.toString()
                val selectedYear = spinnerYear.selectedItem.toString()

                val formattedDate = "$selectedDay $selectedMonth $selectedYear"

                if (userId != null) {
                    firestore.collection("User").document(userId!!)
                        .update("DOB", formattedDate)
                        .addOnSuccessListener {
                            tvDob.text = "DOB: $formattedDate"
                            btnEditDob.visibility = View.GONE
                            Toast.makeText(requireContext(), "Tanggal lahir disimpan!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Gagal menyimpan DOB", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
    }


    private fun loadUserProfile(userId: String) {
        firestore.collection("User").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fName = document.getString("FName") ?: ""
                    val lName = document.getString("LName") ?: ""
                    val email = document.getString("Email") ?: ""
                    val dob = document.get("DOB")
                    val imageUrl = document.getString("imageURL")

                    tvDisplayName.text = "Nama: $fName $lName"
                    tvEmail.text = "Email: $email"

                    // Cek DOB
                    if (dob == null || (dob is String && dob.isEmpty())) {
                        tvDob.text = "DOB: -"
                        btnEditDob.visibility = View.VISIBLE
                    } else {
                        val dobText = when (dob) {
                            is com.google.firebase.Timestamp -> {
                                val date = dob.toDate()
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                sdf.format(date)
                            }
                            is String -> dob
                            else -> dob.toString()
                        }
                        tvDob.text = "DOB: $dobText"
                        btnEditDob.visibility = View.GONE // sembunyikan tombol kalau sudah ada DOB
                    }

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext()).load(imageUrl).into(imgProfile)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal ambil data user", Toast.LENGTH_SHORT).show()
            }
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun captureImageFromCamera() {
        val photoFile = File.createTempFile("profile_${System.currentTimeMillis()}", ".jpg", requireContext().cacheDir)
        cameraPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
        startActivityForResult(intent, CAMERA_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    imageUri = data?.data
                    uploadImageToFirebase()
                }
                CAMERA_REQUEST -> {
                    imageUri = cameraPhotoUri
                    uploadImageToFirebase()
                }
            }
        }
    }

    private fun uploadImageToFirebase() {
        if (userId == null || imageUri == null) return

        val fileRef = storageRef.child("${userId}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    firestore.collection("User").document(userId!!)
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
