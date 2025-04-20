package com.example.appathon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.util.Locale

class SafeRouteActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseHelper: DatabaseHelper
    
    private lateinit var destinationInput: EditText
    private lateinit var searchButton: Button
    private lateinit var currentLocationButton: FloatingActionButton
    private lateinit var policeChip: Chip
    private lateinit var hospitalChip: Chip
    private lateinit var fireStationChip: Chip
    
    private var currentLocation: Location? = null
    private var destination: LatLng? = null
    private var safeZones: List<SafeZone> = emptyList()
    private var nearestSafeZones: List<SafeZone> = emptyList()
    private var safeZoneMarkers: MutableMap<Marker, SafeZone> = mutableMapOf()
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val BOUNDS_PADDING = 200 // Padding for map bounds in pixels
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_route)
        
        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper(this)
        
        // Initialize UI elements
        destinationInput = findViewById(R.id.destinationInput)
        searchButton = findViewById(R.id.searchButton)
        currentLocationButton = findViewById(R.id.currentLocationButton)
        policeChip = findViewById(R.id.policeChip)
        hospitalChip = findViewById(R.id.hospitalChip)
        fireStationChip = findViewById(R.id.fireStationChip)
        
        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Set click listeners
        searchButton.setOnClickListener { searchDestination() }
        currentLocationButton.setOnClickListener { zoomToCurrentLocation() }
        
        // Set toggle listeners for safe zone visibility
        policeChip.setOnCheckedChangeListener { _, isChecked -> toggleSafeZoneVisibility("POLICE", isChecked) }
        hospitalChip.setOnCheckedChangeListener { _, isChecked -> toggleSafeZoneVisibility("HOSPITAL", isChecked) }
        fireStationChip.setOnCheckedChangeListener { _, isChecked -> toggleSafeZoneVisibility("FIRE_STATION", isChecked) }
        
        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.safe_route_navigation)
        
        // Load safe zones from the database
        loadSafeZones()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Configure map settings
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }
        
        // Set marker click listener
        map.setOnMarkerClickListener(this)
        
        // Try to get the user's current location
        enableMyLocation()
        
        // Display safe zones on the map
        displaySafeZones()
    }
    
    override fun onMarkerClick(marker: Marker): Boolean {
        // Check if the marker is a safe zone marker
        val safeZone = safeZoneMarkers[marker]
        if (safeZone != null) {
            showSafeZoneDetails(safeZone)
            return true
        }
        return false
    }
    
    private fun showSafeZoneDetails(safeZone: SafeZone) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_safe_zone, null)
        
        view.findViewById<TextView>(R.id.safeZoneTitle).text = safeZone.name
        view.findViewById<TextView>(R.id.safeZoneType).text = when(safeZone.type) {
            "POLICE" -> getString(R.string.police_stations)
            "HOSPITAL" -> getString(R.string.hospitals)
            "FIRE_STATION" -> getString(R.string.fire_stations)
            else -> safeZone.type
        }
        view.findViewById<TextView>(R.id.safeZoneAddress).text = safeZone.address
        view.findViewById<TextView>(R.id.safeZoneContact).text = safeZone.contactNumber ?: "N/A"
        view.findViewById<TextView>(R.id.safeZoneHours).text = safeZone.operationHours ?: "N/A"
        
        // Calculate distance if current location is available
        currentLocation?.let { location ->
            val distance = calculateDistance(
                location.latitude, location.longitude,
                safeZone.latitude, safeZone.longitude
            )
            view.findViewById<TextView>(R.id.safeZoneDistance).text = 
                getString(R.string.distance_to_safe_zone, distance)
        }
        
        // Set up directions button
        view.findViewById<Button>(R.id.directionsButton).setOnClickListener {
            // Open Google Maps with directions to this safe zone
            val gmmIntentUri = Uri.parse("google.navigation:q=${safeZone.latitude},${safeZone.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // If Google Maps is not installed, open in browser
                val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${safeZone.latitude},${safeZone.longitude}")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                startActivity(browserIntent)
            }
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Snackbar.make(
                    findViewById(R.id.map),
                    R.string.location_permission_required,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.open_settings) {
                    startActivity(Intent().apply {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", packageName, null)
                    })
                }.show()
            }
        }
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                zoomToCurrentLocation()
            }
        }
    }
    
    private fun zoomToCurrentLocation() {
        currentLocation?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } ?: run {
            getCurrentLocation()
        }
    }
    
    private fun loadSafeZones() {
        safeZones = databaseHelper.getAllSafeZones()
    }
    
    private fun displaySafeZones() {
        map.clear()
        safeZoneMarkers.clear()
        
        // Filter safe zones based on chip selections
        val filteredSafeZones = safeZones.filter { zone ->
            when (zone.type) {
                "POLICE" -> policeChip.isChecked
                "HOSPITAL" -> hospitalChip.isChecked
                "FIRE_STATION" -> fireStationChip.isChecked
                else -> true
            }
        }
        
        // Add markers for each safe zone
        for (zone in filteredSafeZones) {
            val position = LatLng(zone.latitude, zone.longitude)
            val markerOptions = MarkerOptions()
                .position(position)
                .title(zone.name)
                .snippet("${zone.address}\n${zone.contactNumber ?: ""}")
            
            // Set custom icon based on type
            when (zone.type) {
                "POLICE" -> markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_police))
                "HOSPITAL" -> markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hospital))
                "FIRE_STATION" -> markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_fire_station))
            }
            
            val marker = map.addMarker(markerOptions)
            if (marker != null) {
                safeZoneMarkers[marker] = zone
            }
        }
        
        // Draw route if destination is set
        destination?.let { dest ->
            drawRouteToDestination(dest)
        }
    }
    
    private fun toggleSafeZoneVisibility(type: String, visible: Boolean) {
        displaySafeZones()
    }
    
    private fun searchDestination() {
        val destinationText = destinationInput.text.toString().trim()
        if (destinationText.isEmpty()) {
            return
        }
        
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(destinationText, 1)?.let { addresses ->
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    destination = LatLng(address.latitude, address.longitude)
                    
                    // Draw route to destination
                    drawRouteToDestination(destination!!)
                    
                    // Show nearby safe zones
                    findAndShowNearbySafeZones(destination!!)
                } else {
                    Toast.makeText(this, R.string.destination_not_found, Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, R.string.destination_not_found, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Geocoder error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun drawRouteToDestination(destination: LatLng) {
        // In a real app, you would use the Directions API to get the route
        // For this example, we'll just draw a straight line
        currentLocation?.let { location ->
            val origin = LatLng(location.latitude, location.longitude)
            
            // Clear existing polylines
            map.clear()
            safeZoneMarkers.clear()
            
            // Re-add safe zone markers
            displaySafeZones()
            
            // Add destination marker
            val destinationMarker = map.addMarker(
                MarkerOptions()
                    .position(destination)
                    .title(destinationInput.text.toString())
            )
            
            // Draw a polyline for the route
            map.addPolyline(
                PolylineOptions()
                    .add(origin, destination)
                    .width(5f)
                    .color(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            )
            
            // Zoom to show both origin and destination
            val bounds = LatLngBounds.Builder()
                .include(origin)
                .include(destination)
                .build()
            
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    BOUNDS_PADDING
                )
            )
            
            // In a real implementation, this would provide directions and duration
            val distance = calculateDistance(
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude
            )
            
            Snackbar.make(
                findViewById(R.id.map),
                String.format(getString(R.string.total_distance), distance),
                Snackbar.LENGTH_LONG
            ).show()
            
            // Show route summary with safety info
            showRouteSummary(distance)
        } ?: run {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun findAndShowNearbySafeZones(location: LatLng) {
        nearestSafeZones = databaseHelper.getNearestSafeZones(
            location.latitude,
            location.longitude,
            5.0, // 5 km radius
            5 // Show up to 5 nearest zones
        )
        
        if (nearestSafeZones.isNotEmpty()) {
            Snackbar.make(
                findViewById(R.id.map),
                getString(R.string.safe_zones_nearby, nearestSafeZones.size),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    private fun showRouteSummary(distanceKm: Double) {
        if (nearestSafeZones.isEmpty()) {
            return
        }
        
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_route_summary, null)
        
        view.findViewById<TextView>(R.id.routeTitle).text = getString(R.string.route_guidance)
        view.findViewById<TextView>(R.id.routeDistance).text = getString(R.string.total_distance, distanceKm)
        
        // Estimate time (rough approximation: 1 km ≈ 12 minutes walking)
        val timeMinutes = (distanceKm * 12).toInt()
        val timeStr = if (timeMinutes > 60) {
            val hours = timeMinutes / 60
            val mins = timeMinutes % 60
            "$hours hr $mins min"
        } else {
            "$timeMinutes min"
        }
        view.findViewById<TextView>(R.id.routeTime).text = getString(R.string.estimated_time, timeStr)
        
        // Show safe zones count
        view.findViewById<TextView>(R.id.safezoneCount).text = getString(R.string.safe_zones_nearby, nearestSafeZones.size)
        
        // List nearest safe zones
        val safezonesContainer = view.findViewById<TextView>(R.id.safezoneList)
        val sb = StringBuilder()
        for ((index, zone) in nearestSafeZones.withIndex()) {
            sb.append("${index + 1}. ${zone.name} (${zone.type})\n")
            sb.append("   ${zone.address}\n")
            
            // Add distance from current location if available
            currentLocation?.let { location ->
                val distance = calculateDistance(
                    location.latitude, location.longitude,
                    zone.latitude, zone.longitude
                )
                sb.append("   ${String.format("%.1f", distance)} km away\n")
            }
            
            if (index < nearestSafeZones.size - 1) {
                sb.append("\n")
            }
        }
        safezonesContainer.text = sb.toString()
        
        // Set up button to view all safe zones
        view.findViewById<Button>(R.id.viewAllSafezonesButton).setOnClickListener {
            dialog.dismiss()
            
            // Zoom out to show all safe zones
            if (nearestSafeZones.isNotEmpty() && currentLocation != null) {
                val boundsBuilder = LatLngBounds.Builder()
                boundsBuilder.include(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
                
                if (destination != null) {
                    boundsBuilder.include(destination!!)
                }
                
                for (zone in nearestSafeZones) {
                    boundsBuilder.include(LatLng(zone.latitude, zone.longitude))
                }
                
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        boundsBuilder.build(),
                        BOUNDS_PADDING
                    )
                )
            }
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Simple Euclidean distance calculation (approximation for small distances)
        val latDiff = lat1 - lat2
        val lonDiff = lon1 - lon2 * Math.cos(Math.toRadians(lat1))
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111.32 // 1 degree is approximately 111.32 km
    }
} 