package com.example.appathon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportIncidentActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var descriptionEditText: EditText
    private lateinit var locationText: TextView
    private lateinit var addressText: TextView
    private lateinit var photoImageView: ImageView
    private lateinit var takePhotoButton: Button
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var userManager: UserManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var currentLocation: Location? = null
    private var currentAddress: String = ""
    private var photoFile: File? = null
    private var photoUri: Uri? = null
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
                    this,
                    "Location permission is required to submit an incident report",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // Camera permission request
    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Camera permission granted, take photo
            dispatchTakePictureIntent()
        } else {
            // Camera permission denied
            showCameraPermissionDeniedDialog()
        }
    }
    
    // Take photo result
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Photo was taken successfully
            photoUri?.let { uri ->
                photoImageView.setImageURI(uri)
                photoImageView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_incident)
        
        // Initialize views
        titleEditText = findViewById(R.id.incident_title_input)
        typeSpinner = findViewById(R.id.incident_type_spinner)
        descriptionEditText = findViewById(R.id.incident_description_input)
        locationText = findViewById(R.id.location_text)
        addressText = findViewById(R.id.address_text)
        photoImageView = findViewById(R.id.incident_photo)
        takePhotoButton = findViewById(R.id.take_photo_button)
        submitButton = findViewById(R.id.submit_report_button)
        progressBar = findViewById(R.id.progress_bar)
        
        // Initialize user manager and database helper
        userManager = UserManager(this)
        databaseHelper = DatabaseHelper(this)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Setup incident type spinner
        val incidentTypes = resources.getStringArray(R.array.incident_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, incidentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
        
        // Set up button click listeners
        takePhotoButton.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }
        
        submitButton.setOnClickListener {
            submitIncidentReport()
        }
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.report_title)
        
        // Get location on startup
        checkLocationPermissionAndGetLocation()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
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
    
    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request camera permission
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        } else {
            // Permission already granted, take photo
            dispatchTakePictureIntent()
        }
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
                    this,
                    "Failed to get location: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                "Security exception: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            
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
    
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                photoFile = createImageFile()
                photoFile?.also { file ->
                    // Create a URI and store it in the member variable
                    val uri = FileProvider.getUriForFile(
                        this,
                        "com.example.appathon.fileprovider",
                        file
                    )
                    photoUri = uri
                    
                    // Use the local variable to launch the camera
                    takePictureLauncher.launch(uri)
                }
            } ?: run {
                Toast.makeText(
                    this,
                    "No camera app found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", 
            ".jpg", 
            storageDir
        )
    }
    
    private fun submitIncidentReport() {
        // Validate inputs
        val title = titleEditText.text.toString().trim()
        val type = typeSpinner.selectedItem.toString()
        val description = descriptionEditText.text.toString().trim()
        
        if (title.isEmpty()) {
            titleEditText.error = "Title is required"
            titleEditText.requestFocus()
            return
        }
        
        if (description.isEmpty()) {
            descriptionEditText.error = "Description is required"
            descriptionEditText.requestFocus()
            return
        }
        
        if (currentLocation == null) {
            Toast.makeText(
                this,
                "Location is required. Please wait for location to be fetched.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        
        // Get photo path if available
        val photoPath = photoFile?.absolutePath
        
        // Save incident to database
        val location = currentLocation!!
        val incidentId = databaseHelper.addIncident(
            title,
            type,
            description,
            location.latitude,
            location.longitude,
            currentAddress,
            photoPath,
            userManager.getCurrentUserName(),
            userId
        )
        
        if (incidentId != -1L) {
            // Success
            Toast.makeText(
                this,
                getString(R.string.incident_reported),
                Toast.LENGTH_SHORT
            ).show()
            
            // Go back
            finish()
        } else {
            // Failed
            progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                getString(R.string.report_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun showCameraPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera_permission_required))
            .setMessage(getString(R.string.camera_permission_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing location requests to prevent memory leaks
        cancellationTokenSource.cancel()
    }
} 