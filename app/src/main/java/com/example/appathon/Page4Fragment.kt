package com.example.appathon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Page4Fragment : Fragment() {

    private lateinit var reportButton: Button
    private lateinit var myReportsButton: Button
    private lateinit var communityReportsButton: Button
    private lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_page4, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        reportButton = view.findViewById(R.id.button_report_incident)
        myReportsButton = view.findViewById(R.id.button_my_reports)
        communityReportsButton = view.findViewById(R.id.button_community_reports)

        // Initialize user manager
        userManager = UserManager(requireContext())

        // Set up button click listeners
        reportButton.setOnClickListener {
            val intent = Intent(requireContext(), ReportIncidentActivity::class.java)
            startActivity(intent)
        }

        myReportsButton.setOnClickListener {
            val intent = Intent(requireContext(), IncidentListActivity::class.java)
            intent.putExtra("SHOW_USER_INCIDENTS", true)
            startActivity(intent)
        }

        communityReportsButton.setOnClickListener {
            val intent = Intent(requireContext(), IncidentListActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        fun newInstance() = Page4Fragment()
    }
} 