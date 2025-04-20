package com.example.appathon

import android.app.AlertDialog
import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var addButton: MaterialButton
    private lateinit var fabAddContact: FloatingActionButton
    private lateinit var contactCountText: TextView
    
    private lateinit var userManager: UserManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var contactsAdapter: EmergencyContactsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contacts)
        
        // Initialize toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize views
        recyclerView = findViewById(R.id.recycler_contacts)
        emptyView = findViewById(R.id.empty_view)
        addButton = findViewById(R.id.button_add_contact)
        fabAddContact = findViewById(R.id.fab_add_contact)
        contactCountText = findViewById(R.id.text_contact_count)
        
        // Initialize database and user manager
        userManager = UserManager(this)
        databaseHelper = DatabaseHelper(this)
        
        // Setup recycler view
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Load contacts for current user
        loadContacts()
        
        // Setup add contact button
        addButton.setOnClickListener {
            showAddContactDialog()
        }
        
        // Setup FAB
        fabAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }
    
    private fun loadContacts() {
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val cursor = databaseHelper.getEmergencyContacts(userId)
        updateEmptyViewVisibility(cursor.count)
        
        contactsAdapter = EmergencyContactsAdapter(cursor)
        recyclerView.adapter = contactsAdapter
        
        // Update contact count
        contactCountText.text = "${cursor.count} contacts"
    }
    
    private fun updateEmptyViewVisibility(contactCount: Int) {
        if (contactCount == 0) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_contact_name)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.edit_contact_phone)
        val relationshipEditText = dialogView.findViewById<EditText>(R.id.edit_relationship)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add", null) // Set to null, we'll override this below
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            addButton.setOnClickListener {
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()
                val relationship = relationshipEditText.text.toString().trim()
                
                if (name.isEmpty()) {
                    nameEditText.error = "Name is required"
                    return@setOnClickListener
                }
                
                if (phone.isEmpty()) {
                    phoneEditText.error = "Phone number is required"
                    return@setOnClickListener
                }
                
                addContact(name, phone, relationship)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showEditContactDialog(id: Long, name: String, phone: String, relationship: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_contact_name)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.edit_contact_phone)
        val relationshipEditText = dialogView.findViewById<EditText>(R.id.edit_relationship)
        
        // Pre-fill with existing data
        nameEditText.setText(name)
        phoneEditText.setText(phone)
        relationshipEditText.setText(relationship)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Update", null) // Set to null, we'll override this below
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val updateButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            updateButton.setOnClickListener {
                val newName = nameEditText.text.toString().trim()
                val newPhone = phoneEditText.text.toString().trim()
                val newRelationship = relationshipEditText.text.toString().trim()
                
                if (newName.isEmpty()) {
                    nameEditText.error = "Name is required"
                    return@setOnClickListener
                }
                
                if (newPhone.isEmpty()) {
                    phoneEditText.error = "Phone number is required"
                    return@setOnClickListener
                }
                
                updateContact(id, newName, newPhone, newRelationship)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun addContact(name: String, phone: String, relationship: String) {
        val userId = userManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        val contactId = databaseHelper.addEmergencyContact(name, phone, userId, relationship)
        
        if (contactId != -1L) {
            Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show()
            
            // Reload contacts
            loadContacts()
        } else {
            Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateContact(id: Long, name: String, phone: String, relationship: String) {
        val result = databaseHelper.updateEmergencyContact(id, name, phone, relationship)
        
        if (result > 0) {
            Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show()
            
            // Reload contacts
            loadContacts()
        } else {
            Toast.makeText(this, "Failed to update contact", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteContact(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this emergency contact?")
            .setPositiveButton("Delete") { _, _ ->
                val result = databaseHelper.deleteEmergencyContact(id)
                
                if (result > 0) {
                    Toast.makeText(this, "Contact deleted successfully", Toast.LENGTH_SHORT).show()
                    
                    // Reload contacts
                    loadContacts()
                } else {
                    Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    inner class EmergencyContactsAdapter(private val cursor: Cursor) : 
            RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder>() {
            
        inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameText: TextView = itemView.findViewById(R.id.text_contact_name)
            val phoneText: TextView = itemView.findViewById(R.id.text_contact_phone)
            val relationshipText: TextView = itemView.findViewById(R.id.text_relationship)
            val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
            val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emergency_contact, parent, false)
            return ContactViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            cursor.moveToPosition(position)
            
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_NAME))
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_PHONE))
            val relationship = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTACT_RELATIONSHIP))
            
            holder.nameText.text = name
            holder.phoneText.text = phone
            
            if (relationship.isNotEmpty()) {
                holder.relationshipText.text = relationship
                holder.relationshipText.visibility = View.VISIBLE
            } else {
                holder.relationshipText.visibility = View.GONE
            }
            
            holder.editButton.setOnClickListener {
                showEditContactDialog(id, name, phone, relationship)
            }
            
            holder.deleteButton.setOnClickListener {
                deleteContact(id)
            }
        }
        
        override fun getItemCount(): Int = cursor.count
    }
} 