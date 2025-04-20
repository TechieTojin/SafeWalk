package com.example.appathon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import java.util.Locale

class Page1Fragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationText: TextView
    private lateinit var addressText: TextView
    private lateinit var getLocationButton: Button
    private lateinit var sosButton: MaterialButton
    private lateinit var reportIncidentButton: Button
    private lateinit var viewReportsButton: Button
    private lateinit var safeRouteButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var userManager: UserManager
    private lateinit var databaseHelper: DatabaseHelper
    
    private var currentLocation: Location? = null
    private var currentAddress: String = ""
    private val cancellationTokenSource = CancellationTokenSource()

    // Location permission request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Permission granted, get location
                getLastLocation()
            }
            else -> {
                // Permission denied
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to show your location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // SMS permission request
    private val smsPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // SMS permission granted, send SOS
            sendSosMessage()
        } else {
            // SMS permission denied
            showSmsPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_page1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        locationText = view.findViewById(R.id.location_text)
        addressText = view.findViewById(R.id.address_text)
        getLocationButton = view.findViewById(R.id.get_location_button)
        sosButton = view.findViewById(R.id.sos_button)
        reportIncidentButton = view.findViewById(R.id.report_incident_button)
        viewReportsButton = view.findViewById(R.id.view_reports_button)
        safeRouteButton = view.findViewById(R.id.safe_route_button)
        progressBar = view.findViewById(R.id.location_progress)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Initialize user manager and database helper
        userManager = UserManager(requireContext())
        databaseHelper = DatabaseHelper(requireContext())

        // Set up button click listeners
        getLocationButton.setOnClickListener {
            checkLocationPermissionAndGetLocation()
        }
        
        sosButton.setOnClickListener {
            activateSos()
        }
        
        reportIncidentButton.setOnClickListener {
            // Navigate to report incident screen
            navigateToReportIncident()
        }
        
        viewReportsButton.setOnClickListener {
            // Navigate to community reports screen
            navigateToCommunityReports()
        }
        
        safeRouteButton.setOnClickListener {
            // Navigate to safe route mapping
            navigateToSafeRoute()
        }
        
        // Get location on startup
        checkLocationPermissionAndGetLocation()
    }

    private fun checkLocationPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permission already granted, get location
            getLastLocation()
        }
    }
    
    private fun checkSmsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestSmsPermission() {
        smsPermissionRequest.launch(Manifest.permission.SEND_SMS)
    }

    private fun getLastLocation() {
        progressBar.visibility = View.VISIBLE
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                progressBar.visibility = View.GONE
                if (location != null) {
                    // Save current location
                    currentLocation = location
                    
                    // Update UI with location data
                    locationText.text = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                    
                    // Get address from location
                    getAddressFromLocation(location.latitude, location.longitude)
                } else {
                    locationText.text = "Location not available"
                    addressText.text = "Address not available"
                }
            }.addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Failed to get location: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            progressBar.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Security exception: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
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
                    
                    currentAddress = addressParts.joinToString(", ")
                    addressText.text = currentAddress
                } else {
                    addressText.text = "No address found"
                    currentAddress = ""
                }
            } ?: run {
                addressText.text = "Geocoder service not available"
                currentAddress = ""
            }
        } catch (e: Exception) {
            addressText.text = "Error getting address: ${e.message}"
            currentAddress = ""
        }
    }
    
    private fun activateSos() {
        // First check if we have a location
        if (currentLocation == null) {
            // No location available, try to get it first
            Toast.makeText(
                requireContext(),
                "Getting your location for emergency...",
                Toast.LENGTH_SHORT
            ).show()
            
            getLastLocation()
            return
        }
        
        // Check for SMS permission
        if (checkSmsPermission()) {
            sendSosMessage()
        } else {
            requestSmsPermission()
        }
    }
    
    private fun sendSosMessage() {
        val location = currentLocation ?: return
        
        // Get emergency contacts
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            // User not logged in
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
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
        
        // Create the SOS message with Google Maps link
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
                    getString(R.string.sos_activated),
                    Toast.LENGTH_LONG
                ).show()
                
                // Visual feedback of activation
                sosButton.alpha = 0.5f
                sosButton.postDelayed({
                    sosButton.alpha = 1.0f
                }, 3000)
            }
            
        } catch (e: Exception) {
            // Handle general SMS failure
            Toast.makeText(
                context,
                "Failed to send SOS: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
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
    
    private fun navigateToReportIncident() {
        // Navigate to incident reporting screen
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(requireContext(), ReportIncidentActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToCommunityReports() {
        // Navigate to incident list screen
        val intent = Intent(requireContext(), IncidentListActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSafeRoute() {
        startActivity(Intent(requireContext(), SafeRouteActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any ongoing location requests to prevent memory leaks
        cancellationTokenSource.cancel()
    }

    companion object {
        fun newInstance() = Page1Fragment()
    }
} 