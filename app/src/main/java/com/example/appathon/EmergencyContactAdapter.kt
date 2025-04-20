package com.example.appathon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying emergency contacts in a RecyclerView
 */
class EmergencyContactAdapter(
    private var contacts: List<EmergencyContact>,
    private val onDeleteClickListener: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact, onDeleteClickListener)
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_contact_name)
        private val phoneTextView: TextView = itemView.findViewById(R.id.text_contact_phone)
        private val relationshipTextView: TextView = itemView.findViewById(R.id.text_relationship)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(contact: EmergencyContact, onDeleteClickListener: (EmergencyContact) -> Unit) {
            nameTextView.text = contact.name
            phoneTextView.text = contact.phone
            
            if (contact.relationship.isNotEmpty()) {
                relationshipTextView.text = contact.relationship
                relationshipTextView.visibility = View.VISIBLE
            } else {
                relationshipTextView.visibility = View.GONE
            }

            deleteButton.setOnClickListener {
                onDeleteClickListener(contact)
            }
        }
    }
} 