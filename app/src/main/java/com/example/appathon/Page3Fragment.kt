package com.example.appathon

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class Page3Fragment : Fragment(), OnMapReadyCallback {

    private lateinit var shareButton: MaterialButton
    private lateinit var shareSmsButton: MaterialButton
    private lateinit var manageContactsButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var coordinatesText: TextView
    private lateinit var addressText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var locationUpdateTimeText: TextView
    private lateinit var mapPermissionText: TextView
    private lateinit var mapPlaceholder: ImageView
    private lateinit var sharingHistoryLayout: LinearLayout
    private lateinit var noHistoryText: TextView
    
    private lateinit var userManager: UserManager
    private lateinit var databaseHelper: DatabaseHelper
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var mMap: GoogleMap? = null
    
    private var isLocationSharing = false
    private var currentLocation: Location? = null
    private var shareHistory = mutableListOf<ShareHistoryEntry>()
    
    // Class to hold share history entries
    data class ShareHistoryEntry(
        val timestamp: Long,
        val method: String,
        val recipients: Int
    )
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                setupMapIfReady()
                startLocationUpdates()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted
                setupMapIfReady()
                startLocationUpdates()
            }
            else -> {
                // No location access granted
                showPermissionDeniedDialog()
                mapPermissionText.visibility = View.VISIBLE
                mapPlaceholder.visibility = View.VISIBLE
            }
        }
    }
    
    private val smsPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // SMS permission granted
            shareLocationViaSms()
        } else {
            // SMS permission denied
            showSmsPermissionDeniedDialog()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_page3, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        shareButton = view.findViewById(R.id.button_share_location)
        shareSmsButton = view.findViewById(R.id.button_share_via_sms)
        manageContactsButton = view.findViewById(R.id.button_manage_contacts)
        statusText = view.findViewById(R.id.text_location_status)
        coordinatesText = view.findViewById(R.id.text_coordinates)
        addressText = view.findViewById(R.id.text_address)
        accuracyText = view.findViewById(R.id.text_accuracy)
        locationUpdateTimeText = view.findViewById(R.id.text_location_update_time)
        mapPermissionText = view.findViewById(R.id.map_permission_text)
        mapPlaceholder = view.findViewById(R.id.image_location_placeholder)
        sharingHistoryLayout = view.findViewById(R.id.layout_sharing_history)
        noHistoryText = view.findViewById(R.id.text_no_history)
        
        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.location_map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Initialize user manager and database helper
        userManager = UserManager(requireContext())
        databaseHelper = DatabaseHelper(requireContext())
        
        // Setup location request
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(10))
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5))
            .setMaxUpdateDelayMillis(TimeUnit.SECONDS.toMillis(15))
            .build()
        
        // Setup location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location)
                }
            }
        }
        
        // Setup share button
        shareButton.setOnClickListener {
            if (isLocationSharing) {
                stopLocationSharing()
            } else {
                startLocationSharing()
            }
        }
        
        // Setup SMS share button
        shareSmsButton.setOnClickListener {
            if (checkSmsPermission()) {
                shareLocationViaSms()
            } else {
                requestSmsPermission()
            }
        }
        
        // Setup manage contacts button
        manageContactsButton.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactsActivity::class.java)
            startActivity(intent)
        }
        
        // Load dummy history data (in a real app, this would come from a database)
        loadDummyShareHistory()
    }
    
    private fun loadDummyShareHistory() {
        // This would normally be loaded from a database
        // For demo purposes, we'll add some dummy entries
        shareHistory.add(ShareHistoryEntry(
            System.currentTimeMillis() - 1000 * 60 * 60 * 24, // Yesterday
            "SMS",
            2
        ))
        
        shareHistory.add(ShareHistoryEntry(
            System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
            "Live Sharing",
            3
        ))
        
        updateShareHistoryUI()
    }
    
    private fun updateShareHistoryUI() {
        if (shareHistory.isEmpty()) {
            noHistoryText.visibility = View.VISIBLE
            return
        }
        
        noHistoryText.visibility = View.GONE
        sharingHistoryLayout.removeAllViews()
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        
        for (entry in shareHistory) {
            val historyItemView = layoutInflater.inflate(
                R.layout.item_share_history, 
                sharingHistoryLayout,
                false
            )
            
            val dateText = historyItemView.findViewById<TextView>(R.id.text_share_date)
            val detailsText = historyItemView.findViewById<TextView>(R.id.text_share_details)
            
            dateText.text = sdf.format(Date(entry.timestamp))
            detailsText.text = "${entry.method} · ${entry.recipients} contacts"
            
            sharingHistoryLayout.addView(historyItemView)
        }
    }
    
    private fun addShareHistoryEntry(method: String, recipients: Int) {
        val entry = ShareHistoryEntry(
            System.currentTimeMillis(),
            method,
            recipients
        )
        
        shareHistory.add(0, entry) // Add to beginning of list
        updateShareHistoryUI()
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapIfReady()
    }
    
    private fun setupMapIfReady() {
        if (mMap == null) return
        
        if (checkLocationPermission()) {
            try {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isZoomControlsEnabled = true
                
                // Hide the placeholder once map is ready
                mapPlaceholder.visibility = View.GONE
                
                // Update map with current location if available
                currentLocation?.let { updateMapLocation(it) }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Location permission error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun updateMapLocation(location: Location) {
        if (mMap == null) return
        
        val latLng = LatLng(location.latitude, location.longitude)
        
        // Clear previous markers
        mMap?.clear()
        
        // Add a marker at the current location
        mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Current Location")
        )
        
        // Move camera to the location
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }
    
    private fun startLocationSharing() {
        if (checkLocationPermission()) {
            isLocationSharing = true
            shareButton.text = getString(R.string.stop_sharing)
            statusText.text = "ACTIVE"
            statusText.setBackgroundResource(R.drawable.status_active_background)
            shareSmsButton.isEnabled = true
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }
    
    private fun stopLocationSharing() {
        isLocationSharing = false
        shareButton.text = getString(R.string.start_sharing)
        statusText.text = getString(R.string.location_not_sharing)
        statusText.setBackgroundResource(R.drawable.status_background)
        stopLocationUpdates()
        
        // Disable SMS button if we're not sharing location
        if (currentLocation == null) {
            shareSmsButton.isEnabled = false
        }
    }
    
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun requestSmsPermission() {
        smsPermissionRequest.launch(Manifest.permission.SEND_SMS)
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.location_permission_required))
            .setMessage(getString(R.string.location_permission_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showSmsPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sms_permission_required))
            .setMessage(getString(R.string.sms_permission_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                
                // Get last known location
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            updateLocationUI(location)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Failed to get location: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Location permission error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    private fun updateLocationUI(location: Location) {
        currentLocation = location
        
        // Format the timestamp
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateTime = sdf.format(Date(location.time))
        
        // Update the map with the new location
        updateMapLocation(location)
        
        // Update UI with location details
        coordinatesText.text = "${location.latitude}, ${location.longitude}"
        accuracyText.text = "${location.accuracy} meters"
        locationUpdateTimeText.text = dateTime
        
        // Get address from location
        getAddressFromLocation(location.latitude, location.longitude)
        
        // If we have a location, enable the SMS button
        shareSmsButton.isEnabled = true
    }
    
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.let { addresses ->
                if (addresses.isNotEmpty()) {
                    val address: Address = addresses[0]
                    val addressParts = mutableListOf<String>()
                    
                    // Get address lines
                    for (i in 0..address.maxAddressLineIndex) {
                        addressParts.add(address.getAddressLine(i))
                    }
                    
                    addressText.text = addressParts.joinToString(", ")
                } else {
                    addressText.text = "No address found"
                }
            } ?: run {
                addressText.text = "Geocoder service not available"
            }
        } catch (e: Exception) {
            addressText.text = "Error getting address: ${e.message}"
        }
    }
    
    private fun shareLocationViaSms() {
        val location = currentLocation ?: return
        
        // Get emergency contacts
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            // User not logged in
            return
        }
        
        val cursor = databaseHelper.getEmergencyContacts(userId)
        val contactPhones = mutableListOf<String>()
        
        if (cursor.moveToFirst()) {
            do {
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_PHONE))
                contactPhones.add(phone)
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        if (contactPhones.isEmpty()) {
            Toast.makeText(
                context,
                getString(R.string.no_contacts_to_share),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // Create the message with Google Maps link
        val message = getString(
            R.string.location_message,
            location.latitude,
            location.longitude,
            location.accuracy.toInt()
        )
        
        try {
            // Get the default SMS manager
            val smsManager = SmsManager.getDefault()
            
            // Send SMS to each contact
            var sentCount = 0
            
            for (phone in contactPhones) {
                try {
                    // Send the SMS
                    smsManager.sendTextMessage(
                        phone,
                        null,
                        message,
                        null,
                        null
                    )
                    sentCount++
                } catch (e: Exception) {
                    // Handle sending failure for this contact
                    Toast.makeText(
                        context,
                        "Failed to send SMS to $phone: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            // Show success message
            if (sentCount > 0) {
                Toast.makeText(
                    context,
                    getString(R.string.sms_sent_successfully, sentCount),
                    Toast.LENGTH_LONG
                ).show()
                
                // Add to share history
                addShareHistoryEntry("SMS", sentCount)
            }
            
        } catch (e: Exception) {
            // Handle general SMS failure
            Toast.makeText(
                context,
                "Failed to send SMS: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (isLocationSharing && checkLocationPermission()) {
            startLocationUpdates()
        }
        setupMapIfReady()
    }
    
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    
    companion object {
        fun newInstance() = Page3Fragment()
    }
} 