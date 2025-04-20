package com.example.appathon

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {
    
    private lateinit var userManager: UserManager
    private lateinit var sharedPreferences: SharedPreferences
    
    // Preferences keys
    companion object {
        const val PREF_NAME = "safewalk_preferences"
        const val PREF_SOS_SENSITIVITY = "sos_sensitivity"
        const val PREF_LOCATION_ENABLED = "location_enabled"
        const val PREF_INCOGNITO_MODE = "incognito_mode"
        const val PREF_EMERGENCY_ALERTS = "emergency_alerts"
        const val PREF_INCIDENT_REPORTS = "incident_reports"
        const val PREF_COMMUNITY_MESSAGES = "community_messages"
        const val PREF_DARK_MODE = "dark_mode"
        const val PREF_TEXT_SIZE = "text_size"
        
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userManager = UserManager.getInstance(requireContext())
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        setupUserProfile(view)
        setupPreferences(view)
        setupButtons(view)
    }
    
    private fun setupUserProfile(view: View) {
        val profileEmail = view.findViewById<TextView>(R.id.profile_email)
        val currentUser = userManager.getCurrentUser()
        if (currentUser != null) {
            profileEmail.text = currentUser.email
        } else {
            profileEmail.text = userManager.getCurrentUserEmail()
        }
        
        // Setup profile click listener
        val profileSetting = view.findViewById<LinearLayout>(R.id.profile_setting)
        profileSetting.setOnClickListener {
            Toast.makeText(requireContext(), "Profile settings coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Setup password change click listener
        val changePassword = view.findViewById<LinearLayout>(R.id.change_password)
        changePassword.setOnClickListener {
            Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupPreferences(view: View) {
        // SOS Sensitivity
        val sosSensitivity = view.findViewById<SeekBar>(R.id.sos_sensitivity)
        sosSensitivity.progress = sharedPreferences.getInt(PREF_SOS_SENSITIVITY, 5)
        sosSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update preference when user changes the seek bar
                if (fromUser) {
                    sharedPreferences.edit().putInt(PREF_SOS_SENSITIVITY, progress).apply()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Manage Emergency Contacts
        val manageEmergencyContacts = view.findViewById<LinearLayout>(R.id.manage_emergency_contacts)
        manageEmergencyContacts.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactsActivity::class.java)
            startActivity(intent)
        }
        
        // Location Services Switch
        val locationSwitch = view.findViewById<SwitchMaterial>(R.id.location_switch)
        locationSwitch.isChecked = sharedPreferences.getBoolean(PREF_LOCATION_ENABLED, true)
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_LOCATION_ENABLED, isChecked).apply()
            if (!isChecked) {
                Toast.makeText(
                    requireContext(),
                    R.string.location_disabled_warning,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        // Incognito Mode Switch
        val incognitoSwitch = view.findViewById<SwitchMaterial>(R.id.incognito_switch)
        incognitoSwitch.isChecked = sharedPreferences.getBoolean(PREF_INCOGNITO_MODE, false)
        incognitoSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_INCOGNITO_MODE, isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) R.string.incognito_enabled else R.string.incognito_disabled,
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Privacy Policy
        val privacyPolicy = view.findViewById<LinearLayout>(R.id.privacy_policy)
        privacyPolicy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy policy will open in browser", Toast.LENGTH_SHORT).show()
            // Would implement browser intent here
        }
        
        // Notification Switches
        setupNotificationSwitches(view)
        
        // Appearance Settings
        setupAppearanceSettings(view)
    }
    
    private fun setupNotificationSwitches(view: View) {
        // Emergency Alerts Switch
        val emergencyAlertsSwitch = view.findViewById<SwitchMaterial>(R.id.emergency_alerts_switch)
        emergencyAlertsSwitch.isChecked = sharedPreferences.getBoolean(PREF_EMERGENCY_ALERTS, true)
        emergencyAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_EMERGENCY_ALERTS, isChecked).apply()
        }
        
        // Incident Reports Switch
        val incidentReportsSwitch = view.findViewById<SwitchMaterial>(R.id.incident_reports_switch)
        incidentReportsSwitch.isChecked = sharedPreferences.getBoolean(PREF_INCIDENT_REPORTS, true)
        incidentReportsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_INCIDENT_REPORTS, isChecked).apply()
        }
        
        // Community Messages Switch
        val communitySwitch = view.findViewById<SwitchMaterial>(R.id.community_switch)
        communitySwitch.isChecked = sharedPreferences.getBoolean(PREF_COMMUNITY_MESSAGES, false)
        communitySwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_COMMUNITY_MESSAGES, isChecked).apply()
        }
    }
    
    private fun setupAppearanceSettings(view: View) {
        // Dark Mode Switch
        val darkModeSwitch = view.findViewById<SwitchMaterial>(R.id.dark_mode_switch)
        darkModeSwitch.isChecked = sharedPreferences.getBoolean(PREF_DARK_MODE, false)
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(PREF_DARK_MODE, isChecked).apply()
            Toast.makeText(
                requireContext(),
                R.string.theme_restart,
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Text Size Radio Group
        val textSizeGroup = view.findViewById<RadioGroup>(R.id.text_size_group)
        val savedTextSize = sharedPreferences.getString(PREF_TEXT_SIZE, "medium")
        
        when (savedTextSize) {
            "small" -> view.findViewById<RadioButton>(R.id.text_size_small).isChecked = true
            "medium" -> view.findViewById<RadioButton>(R.id.text_size_medium).isChecked = true
            "large" -> view.findViewById<RadioButton>(R.id.text_size_large).isChecked = true
        }
        
        textSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val textSize = when (checkedId) {
                R.id.text_size_small -> "small"
                R.id.text_size_medium -> "medium"
                R.id.text_size_large -> "large"
                else -> "medium"
            }
            sharedPreferences.edit().putString(PREF_TEXT_SIZE, textSize).apply()
            Toast.makeText(
                requireContext(),
                R.string.text_size_restart,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun setupButtons(view: View) {
        // Terms of Service
        val termsOfService = view.findViewById<LinearLayout>(R.id.terms_of_service)
        termsOfService.setOnClickListener {
            Toast.makeText(requireContext(), "Terms of service will open in browser", Toast.LENGTH_SHORT).show()
            // Would implement browser intent here
        }
        
        // Logout Button
        val logoutButton = view.findViewById<MaterialButton>(R.id.logout_button)
        logoutButton.setOnClickListener {
            userManager.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
} 