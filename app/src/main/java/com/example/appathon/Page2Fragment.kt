package com.example.appathon

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Page2Fragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var addButton: Button
    private lateinit var adapter: EmergencyContactAdapter
    private lateinit var userManager: UserManager
    private lateinit var databaseHelper: DatabaseHelper
    private val emergencyContacts = mutableListOf<EmergencyContact>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_page2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_emergency_contacts)
        emptyView = view.findViewById(R.id.empty_view)
        addButton = view.findViewById(R.id.button_add_contact)

        // Initialize UserManager and DatabaseHelper
        userManager = UserManager(requireContext())
        databaseHelper = DatabaseHelper(requireContext())

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = EmergencyContactAdapter(emergencyContacts) { contact ->
            deleteContact(contact)
        }
        recyclerView.adapter = adapter

        // Set up add button
        addButton.setOnClickListener {
            showAddContactDialog()
        }

        // Load contacts
        loadEmergencyContacts()
    }

    private fun loadEmergencyContacts() {
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            // User not logged in
            return
        }

        emergencyContacts.clear()
        val cursor = databaseHelper.getEmergencyContacts(userId)
        
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_NAME))
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_PHONE))
                
                val contact = EmergencyContact(id, name, phone, userId)
                emergencyContacts.add(contact)
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        // Update UI
        updateEmptyView()
        adapter.updateContacts(emergencyContacts)
    }

    private fun showAddContactDialog() {
        // Check if max contacts reached
        if (emergencyContacts.size >= 5) {
            Toast.makeText(
                requireContext(),
                "You can only add up to 5 emergency contacts",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Inflate dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.name_input)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.phone_input)
        val nameLayout = dialogView.findViewById<TextInputLayout>(R.id.name_layout)
        val phoneLayout = dialogView.findViewById<TextInputLayout>(R.id.phone_layout)
        
        // Create dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set button click listeners
        dialogView.findViewById<Button>(R.id.button_save).setOnClickListener {
            // Validate inputs
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            var isValid = true
            
            if (name.isEmpty()) {
                nameLayout.error = "Name is required"
                isValid = false
            } else {
                nameLayout.error = null
            }
            
            if (phone.isEmpty()) {
                phoneLayout.error = "Phone number is required"
                isValid = false
            } else {
                phoneLayout.error = null
            }
            
            if (isValid) {
                // Save contact
                addContact(name, phone)
                dialog.dismiss()
            }
        }
        
        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun addContact(name: String, phone: String) {
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            // User not logged in
            return
        }
        
        // Add to database
        val contactId = databaseHelper.addEmergencyContact(name, phone, userId)
        
        if (contactId != -1L) {
            // Add to list
            val newContact = EmergencyContact(contactId, name, phone, userId)
            emergencyContacts.add(newContact)
            
            // Update UI
            updateEmptyView()
            adapter.updateContacts(emergencyContacts)
            
            Toast.makeText(
                requireContext(),
                "Emergency contact added successfully",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to add emergency contact",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun deleteContact(contact: EmergencyContact) {
        // Confirm deletion
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this emergency contact?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete from database
                val result = databaseHelper.deleteEmergencyContact(contact.id)
                
                if (result > 0) {
                    // Remove from list
                    emergencyContacts.remove(contact)
                    
                    // Update UI
                    updateEmptyView()
                    adapter.updateContacts(emergencyContacts)
                    
                    Toast.makeText(
                        requireContext(),
                        "Emergency contact deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete emergency contact",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyView() {
        if (emergencyContacts.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    companion object {
        fun newInstance() = Page2Fragment()
    }
} 