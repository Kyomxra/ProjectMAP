package com.example.projectmap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val transactions = mutableListOf<TransactionWithLocation>()
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvTransactionCount: TextView
    private lateinit var tvMostFrequentLocation: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var userMarker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get userId from SharedPreferences
        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        userId = prefs.getString("userId", null)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Initialize views
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent)
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount)
        tvMostFrequentLocation = view.findViewById(R.id.tvMostFrequentLocation)

        if (userId == null) {
            Toast.makeText(requireContext(), "Belum login! Silakan login dulu.", Toast.LENGTH_LONG).show()
            return
        }

        // Check location permission
        checkLocationPermission()

        // Load transaction data from Firestore
        loadTransactionData()

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                android.util.Log.d("LocationFragment", "Current location: ${location.latitude}, ${location.longitude}")

                // Update map if ready
                if (::map.isInitialized) {
                    showUserLocation()
                }
            } else {
                android.util.Log.d("LocationFragment", "Location is null")
                Toast.makeText(requireContext(), "Tidak dapat menemukan lokasi saat ini", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("LocationFragment", "Failed to get location: ${e.message}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Enable zoom controls
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        // Show user location if available
        if (currentLocation != null) {
            showUserLocation()
        }

        if (transactions.isNotEmpty()) {
            displayTransactions()
            updateStatistics()
        } else {
            // Show user location or default to Jakarta
            if (currentLocation != null) {
                val userLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
            } else {
                val jakarta = LatLng(-6.200000, 106.816666)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 12f))
            }
        }
    }

    private fun showUserLocation() {
        if (currentLocation == null || !::map.isInitialized) return

        val userPosition = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)

        // Remove old marker if exists
        userMarker?.remove()

        // Add custom user location marker (blue circle with person icon)
        userMarker = map.addMarker(
            MarkerOptions()
                .position(userPosition)
                .title("Lokasi Kamu")
                .snippet("Kamu berada di sini")
                .icon(createUserLocationMarker())
                .zIndex(1000f) // Make sure it's on top
        )

        android.util.Log.d("LocationFragment", "User marker added at: ${userPosition.latitude}, ${userPosition.longitude}")
    }

    private fun createUserLocationMarker(): BitmapDescriptor {
        val size = 70
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer circle (light blue glow)
        val glowPaint = Paint().apply {
            color = Color.argb(80, 33, 150, 243)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, glowPaint)

        // Main circle (blue)
        val mainPaint = Paint().apply {
            color = Color.rgb(33, 150, 243) // Blue
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, mainPaint)

        // White border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f - 2, borderPaint)

        // Inner dot
        val dotPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 8f, dotPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun loadTransactionData() {
        if (userId == null) return

        android.util.Log.d("LocationFragment", "Loading transactions for user: $userId")

        firestore.collection("Transactions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("type", "expense") // Only show expenses
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("LocationFragment", "Listen failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Gagal memuat data lokasi", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    android.util.Log.d("LocationFragment", "Snapshots null")
                    return@addSnapshotListener
                }

                android.util.Log.d("LocationFragment", "Found ${snapshots.size()} transactions")

                transactions.clear()

                for (doc in snapshots) {
                    try {
                        val name = doc.getString("note") ?: doc.getString("category") ?: "Transaksi"
                        val category = doc.getString("category") ?: "Lainnya"
                        val amount = (doc.getLong("amount") ?: 0L).toDouble()
                        val timestamp = doc.getTimestamp("date")

                        // Check if transaction has location data
                        val latitude = doc.getDouble("latitude")
                        val longitude = doc.getDouble("longitude")

                        // Only add transactions with valid location
                        if (latitude != null && longitude != null) {
                            val dateStr = if (timestamp != null) {
                                SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(timestamp.toDate())
                            } else {
                                "Unknown date"
                            }

                            transactions.add(
                                TransactionWithLocation(
                                    id = doc.id,
                                    name = name,
                                    latitude = latitude,
                                    longitude = longitude,
                                    amount = amount,
                                    category = category,
                                    date = dateStr,
                                    userId = userId!!,
                                    type = "expense"
                                )
                            )
                        }
                    } catch (ex: Exception) {
                        android.util.Log.e("LocationFragment", "Error parsing transaction: ${ex.message}")
                    }
                }

                android.util.Log.d("LocationFragment", "Loaded ${transactions.size} transactions with location")

                // Update map if ready
                if (::map.isInitialized) {
                    displayTransactions()
                    updateStatistics()
                }
            }
    }

    private fun displayTransactions() {
        // Clear existing markers except user marker
        map.clear()

        // Re-add user marker
        if (currentLocation != null) {
            showUserLocation()
        }

        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), "Belum ada transaksi dengan lokasi", Toast.LENGTH_SHORT).show()
            return
        }

        val bounds = LatLngBounds.Builder()

        // Include user location in bounds
        if (currentLocation != null) {
            bounds.include(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
        }

        val locationFrequency = mutableMapOf<String, Int>()

        transactions.forEach { transaction ->
            val position = LatLng(transaction.latitude, transaction.longitude)

            // Add marker with custom color based on category
            val markerColor = getCategoryColor(transaction.category)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(transaction.name)
                    .snippet("${formatCurrency(transaction.amount)} - ${transaction.category}\n${transaction.date}")
                    .icon(createCustomMarker(markerColor, transaction.amount))
            )

            // Track location frequency
            val locationKey = "${transaction.latitude},${transaction.longitude}"
            locationFrequency[locationKey] = locationFrequency.getOrDefault(locationKey, 0) + 1

            bounds.include(position)
        }

        // Add info window click listener
        map.setOnInfoWindowClickListener { marker ->
            if (marker.title != "Lokasi Kamu") {
                showTransactionDetail(marker.title ?: "")
            }
        }

        // Move camera to show all markers
        try {
            val padding = 150
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding))
        } catch (e: Exception) {
            // Fallback
            if (currentLocation != null) {
                val userLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
            } else {
                val jakarta = LatLng(-6.200000, 106.816666)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 12f))
            }
        }

        // Draw heatmap circles for frequent locations
        drawHeatmapCircles(locationFrequency)
    }

    private fun drawHeatmapCircles(locationFrequency: Map<String, Int>) {
        val maxFrequency = locationFrequency.values.maxOrNull() ?: 1

        locationFrequency.forEach { (locationKey, frequency) ->
            if (frequency > 1) { // Only show circles for locations visited more than once
                val coords = locationKey.split(",")
                val position = LatLng(coords[0].toDouble(), coords[1].toDouble())

                // Circle size and opacity based on frequency
                val radius = 200.0 + (frequency * 100.0)
                val fillColor = Color.argb(
                    (50 + (frequency.toFloat() / maxFrequency * 80)).toInt(),
                    255, 0, 0
                )

                map.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(radius)
                        .strokeColor(Color.argb(180, 255, 0, 0))
                        .strokeWidth(2f)
                        .fillColor(fillColor)
                )
            }
        }
    }

    private fun createCustomMarker(color: Int, amount: Double): BitmapDescriptor {
        val size = when {
            amount >= 1000000 -> 60 // Large marker for expensive transactions
            amount >= 100000 -> 50
            else -> 40 // Small marker for cheap transactions
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Draw circle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw white border
        paint.apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "makan", "makanan", "food & drink" -> Color.rgb(255, 152, 0) // Orange
            "transport", "transportasi" -> Color.rgb(33, 150, 243) // Blue
            "belanja", "shopping" -> Color.rgb(156, 39, 176) // Purple
            "hiburan", "entertainment" -> Color.rgb(233, 30, 99) // Pink
            "bills", "tagihan" -> Color.rgb(76, 175, 80) // Green
            else -> Color.rgb(158, 158, 158) // Gray
        }
    }

    private fun updateStatistics() {
        // Calculate total spent
        val totalSpent = transactions.sumOf { it.amount }
        tvTotalSpent.text = formatCurrency(totalSpent)

        // Count transactions
        tvTransactionCount.text = "${transactions.size} transaksi"

        // Find most frequent location
        val locationCounts = transactions.groupingBy { it.name }.eachCount()
        val mostFrequent = locationCounts.maxByOrNull { it.value }
        tvMostFrequentLocation.text = mostFrequent?.key ?: "-"
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace("Rp", "Rp ")
    }

    private fun showTransactionDetail(name: String) {
        Toast.makeText(requireContext(), "Detail: $name", Toast.LENGTH_SHORT).show()
        // TODO: Implement navigation to detail screen
    }
}