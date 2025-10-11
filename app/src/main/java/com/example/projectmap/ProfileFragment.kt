package com.example.projectmap

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Spinner
import java.util.Calendar
import android.Manifest
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ProfileFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private lateinit var imgBanner: ImageView
    private lateinit var btnChangePhoto: ImageView
    private lateinit var btnChangeBanner: FloatingActionButton
    private lateinit var tvUserId: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDob: TextView
    private lateinit var btnEditDob: Button

    private val PICK_IMAGE_REQUEST = 1001
    private val CAMERA_REQUEST = 1002
    private val PICK_BANNER_REQUEST = 1003
    private val CAMERA_BANNER_REQUEST = 1004
    private val CAMERA_PERMISSION_REQUEST = 100
    private var imageUri: Uri? = null
    private var isBannerUpload = false

    private val firestore = FirebaseFirestore.getInstance()

    private var userId: String? = null
    private var cameraPhotoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        imgProfile = view.findViewById(R.id.imgProfile)
        imgBanner = view.findViewById(R.id.imgBanner)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        btnChangeBanner = view.findViewById(R.id.btnChangeBanner)
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
            isBannerUpload = false
            showImagePickerDialog()
        }

        btnChangeBanner.setOnClickListener {
            isBannerUpload = true
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

        val days = (1..31).map { it.toString() }
        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (1950..currentYear).map { it.toString() }.reversed()

        spinnerDay.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        spinnerMonth.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spinnerYear.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

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
                    tvUserId.text = "User ID: $userId"

                    val fName = document.getString("FName") ?: ""
                    val lName = document.getString("LName") ?: ""
                    val email = document.getString("Email") ?: ""
                    val dob = document.get("DOB")
                    val imageBase64 = document.getString("imageURL")
                    val bannerBase64 = document.getString("bannerURL")

                    tvDisplayName.text = "Nama: $fName $lName"
                    tvEmail.text = "Email: $email"

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
                        btnEditDob.visibility = View.GONE
                    }

                    // Load Base64 image
                    if (!imageBase64.isNullOrEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            imgProfile.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal decode gambar", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load Base64 banner
                    if (!bannerBase64.isNullOrEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(bannerBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            imgBanner.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal decode banner", Toast.LENGTH_SHORT).show()
                        }
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
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            return
        }

        // Permission already granted, proceed with camera
        launchCamera()
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("profile_${System.currentTimeMillis()}", ".jpg", requireContext().cacheDir)
            cameraPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
            startActivityForResult(intent, CAMERA_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, launch camera
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST, PICK_BANNER_REQUEST -> {
                    imageUri = data?.data
                    convertAndUploadImage()
                }
                CAMERA_REQUEST, CAMERA_BANNER_REQUEST -> {
                    imageUri = cameraPhotoUri
                    convertAndUploadImage()
                }
            }
        }
    }

    private fun convertAndUploadImage() {
        if (userId == null || imageUri == null) return

        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Resize bitmap based on type
            val resizedBitmap = if (isBannerUpload) {
                // Banner: wider aspect ratio (e.g., 1200x400)
                resizeBitmap(bitmap, 1200, 400)
            } else {
                // Profile: square (e.g., 800x800)
                resizeBitmap(bitmap, 800, 800)
            }

            // Convert to Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // Upload to Firestore with correct field name
            val fieldName = if (isBannerUpload) "bannerURL" else "imageURL"

            firestore.collection("User").document(userId!!)
                .update(fieldName, base64String)
                .addOnSuccessListener {
                    if (isBannerUpload) {
                        imgBanner.setImageBitmap(resizedBitmap)
                        Toast.makeText(requireContext(), "Banner diperbarui!", Toast.LENGTH_SHORT).show()
                    } else {
                        imgProfile.setImageBitmap(resizedBitmap)
                        Toast.makeText(requireContext(), "Foto profil diperbarui!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal update foto", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // For banners, crop to fit aspect ratio
        return if (maxWidth > maxHeight) {
            // Banner mode: maintain aspect ratio and crop
            val targetAspect = maxWidth.toFloat() / maxHeight.toFloat()
            val currentAspect = width.toFloat() / height.toFloat()

            val (cropWidth, cropHeight) = if (currentAspect > targetAspect) {
                // Image is wider than target - crop width
                val calculatedWidth = (height * targetAspect).toInt()
                Pair(calculatedWidth, height)
            } else {
                // Image is taller than target - crop height
                val calculatedHeight = (width / targetAspect).toInt()
                Pair(width, calculatedHeight)
            }

            // Center crop
            val xOffset = (width - cropWidth) / 2
            val yOffset = (height - cropHeight) / 2

            val croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, cropWidth, cropHeight)
            Bitmap.createScaledBitmap(croppedBitmap, maxWidth, maxHeight, true)
        } else {
            // Profile picture mode: square crop from center
            val size = minOf(width, height)
            val xOffset = (width - size) / 2
            val yOffset = (height - size) / 2

            val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
            Bitmap.createScaledBitmap(squareBitmap, maxWidth, maxHeight, true)
        }
    }
}