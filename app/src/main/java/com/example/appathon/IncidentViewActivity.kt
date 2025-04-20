package com.example.appathon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class IncidentViewActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var typeTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var reportedByTextView: TextView
    private lateinit var reportedAtTextView: TextView
    private lateinit var photoImageView: ImageView
    private lateinit var openMapButton: Button
    private lateinit var shareSmsButton: Button
    private lateinit var databaseHelper: DatabaseHelper
    
    private var incidentId: Long = -1
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address: String = ""
    private var incidentTitle: String = ""
    private var incidentType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident_view)
        
        // Initialize views
        titleTextView = findViewById(R.id.incident_title)
        typeTextView = findViewById(R.id.incident_type)
        descriptionTextView = findViewById(R.id.incident_description)
        locationTextView = findViewById(R.id.incident_location)
        addressTextView = findViewById(R.id.incident_address)
        reportedByTextView = findViewById(R.id.reported_by)
        reportedAtTextView = findViewById(R.id.reported_at)
        photoImageView = findViewById(R.id.incident_photo)
        openMapButton = findViewById(R.id.open_map_button)
        shareSmsButton = findViewById(R.id.share_sms_button)
        
        // Initialize database helper
        databaseHelper = DatabaseHelper(this)
        
        // Get incident ID from intent
        incidentId = intent.getLongExtra("INCIDENT_ID", -1)
        
        if (incidentId == -1L) {
            // No incident ID provided, finish activity
            finish()
            return
        }
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.incident_details)
        
        // Load incident details
        loadIncidentDetails()
        
        // Set up button click listeners
        openMapButton.setOnClickListener {
            openLocationInMap()
        }
        
        shareSmsButton.setOnClickListener {
            shareViaSms()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun loadIncidentDetails() {
        val incident = databaseHelper.getIncidentById(incidentId)
        
        if (incident != null) {
            // Store incident data for later use
            latitude = incident.latitude
            longitude = incident.longitude
            address = incident.address
            incidentTitle = incident.title
            incidentType = incident.type
            
            // Set text views
            titleTextView.text = incident.title
            typeTextView.text = incident.type
            descriptionTextView.text = incident.description
            locationTextView.text = "Lat: ${incident.latitude}, Lng: ${incident.longitude}"
            addressTextView.text = incident.address
            reportedByTextView.text = incident.reportedBy
            
            // Format date
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
            reportedAtTextView.text = dateFormat.format(incident.reportedAt)
            
            // Load photo if available
            incident.photoPath?.let { path ->
                val photoFile = File(path)
                if (photoFile.exists()) {
                    photoImageView.setImageURI(Uri.fromFile(photoFile))
                    photoImageView.visibility = View.VISIBLE
                }
            }
        } else {
            // Incident not found, finish activity
            finish()
        }
    }
    
    private fun openLocationInMap() {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(incidentTitle)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Google Maps not installed, open in browser
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            startActivity(browserIntent)
        }
    }
    
    private fun shareViaSms() {
        val message = getString(
            R.string.incident_share_message,
            incidentTitle,
            incidentType,
            address,
            latitude,
            longitude
        )
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", message)
        }
        
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // SMS app not found, copy to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Incident Details", message)
            clipboard.setPrimaryClip(clip)
            
            // Show toast
            android.widget.Toast.makeText(
                this,
                getString(R.string.copied_to_clipboard),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
} 