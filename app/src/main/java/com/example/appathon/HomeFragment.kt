package com.example.appathon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private lateinit var userManager: UserManager
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UserManager and DatabaseHelper
        userManager = UserManager(requireContext())
        databaseHelper = DatabaseHelper(requireContext())

        // Set welcome message with user's name
        val welcomeText = view.findViewById<TextView>(R.id.welcome_text)
        welcomeText.text = getString(R.string.welcome_user, userManager.getCurrentUserName())

        // Setup click listeners for feature cards
        setupCardClickListeners()
    }

    private fun setupCardClickListeners() {
        // SOS Alert Card
        val sosCard = view?.findViewById<CardView>(R.id.card_sos)
        sosCard?.setOnClickListener {
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Page2Fragment.newInstance())
                .commit()
            mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_sos
        }

        // Emergency Contacts Card
        val contactsCard = view?.findViewById<CardView>(R.id.card_contacts)
        contactsCard?.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactsActivity::class.java)
            startActivity(intent)
        }

        // Live Location Card
        val liveLocationCard = view?.findViewById<CardView>(R.id.card_location)
        liveLocationCard?.setOnClickListener {
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Page3Fragment.newInstance())
                .commit()
            mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_safety
        }

        // Safe Routes Card
        val safeRoutesCard = view?.findViewById<CardView>(R.id.card_safe_route)
        safeRoutesCard?.setOnClickListener {
            navigateToSafeRoute()
        }

        // Report Incident Card
        val reportIncidentCard = view?.findViewById<CardView>(R.id.card_report)
        reportIncidentCard?.setOnClickListener {
            navigateToReportIncident()
        }

        // Community Reports Card
        val communityReportsCard = view?.findViewById<CardView>(R.id.card_community)
        communityReportsCard?.setOnClickListener {
            navigateToCommunityReports()
        }
    }

    private fun navigateToSafeRoute() {
        try {
            // Make sure we have some safe zones (add a couple if we don't have any)
            ensureSafeZonesExist()
            
            val intent = Intent(requireContext(), SafeRouteActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening Safe Route: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun navigateToReportIncident() {
        try {
            val intent = Intent(requireContext(), ReportIncidentActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening Report Incident: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun navigateToCommunityReports() {
        try {
            val intent = Intent(requireContext(), IncidentListActivity::class.java)
            startActivity(intent)
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_community
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error opening Community Reports: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun ensureSafeZonesExist() {
        // Check if we have any safe zones already
        val existingSafeZones = databaseHelper.getAllSafeZones()
        
        if (existingSafeZones.isEmpty()) {
            // Add some sample safe zones
            databaseHelper.addSafeZone(
                SafeZone(
                    id = 1,
                    name = "City Police Station",
                    type = "POLICE",
                    latitude = 40.712776,
                    longitude = -74.005974,
                    address = "123 Main Street, New York",
                    contactNumber = "911",
                    operationHours = "24/7",
                    iconResId = R.drawable.ic_police
                )
            )
            
            databaseHelper.addSafeZone(
                SafeZone(
                    id = 2,
                    name = "Central Hospital",
                    type = "HOSPITAL",
                    latitude = 40.714776,
                    longitude = -74.009974,
                    address = "456 Health Avenue, New York",
                    contactNumber = "212-555-1234",
                    operationHours = "24/7",
                    iconResId = R.drawable.ic_hospital
                )
            )
            
            databaseHelper.addSafeZone(
                SafeZone(
                    id = 3,
                    name = "Main Fire Station",
                    type = "FIRE_STATION",
                    latitude = 40.716776,
                    longitude = -74.002974,
                    address = "789 Rescue Road, New York",
                    contactNumber = "212-555-9876",
                    operationHours = "24/7",
                    iconResId = R.drawable.ic_fire_station
                )
            )
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
} 