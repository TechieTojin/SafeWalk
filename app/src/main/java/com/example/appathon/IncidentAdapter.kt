package com.example.appathon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentAdapter(
    private var incidents: List<Incident>,
    private val listener: IncidentClickListener
) : RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder>() {

    interface IncidentClickListener {
        fun onIncidentClick(incidentId: Long)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident, parent, false)
        return IncidentViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        val incident = incidents[position]
        holder.bind(incident)
    }

    override fun getItemCount(): Int = incidents.size

    fun updateIncidents(newIncidents: List<Incident>) {
        this.incidents = newIncidents
        notifyDataSetChanged()
    }

    inner class IncidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.incident_title)
        private val typeTextView: TextView = itemView.findViewById(R.id.incident_type)
        private val addressTextView: TextView = itemView.findViewById(R.id.incident_address)
        private val dateTextView: TextView = itemView.findViewById(R.id.incident_date)
        private val reportedByTextView: TextView = itemView.findViewById(R.id.reported_by)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onIncidentClick(incidents[position].id)
                }
            }
        }

        fun bind(incident: Incident) {
            titleTextView.text = incident.title
            typeTextView.text = incident.type
            addressTextView.text = incident.address
            
            // Format date
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateTextView.text = dateFormat.format(incident.reportedAt)
            
            reportedByTextView.text = incident.reportedBy
        }
    }
} 