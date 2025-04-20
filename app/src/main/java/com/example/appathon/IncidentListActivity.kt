package com.example.appathon

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class IncidentListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var userManager: UserManager
    private lateinit var incidentAdapter: IncidentAdapter
    
    private var showOnlyUserIncidents = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident_list)
        
        // Initialize views
        recyclerView = findViewById(R.id.incidents_recycler_view)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        emptyView = findViewById(R.id.empty_view)
        
        // Initialize database helper and user manager
        databaseHelper = DatabaseHelper(this)
        userManager = UserManager(this)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.community_reports)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Check if we should show only user incidents
        showOnlyUserIncidents = intent.getBooleanExtra("SHOW_USER_INCIDENTS", false)
        
        if (showOnlyUserIncidents) {
            supportActionBar?.title = getString(R.string.my_reports)
        }
        
        // Initialize adapter
        incidentAdapter = IncidentAdapter(ArrayList(), object : IncidentAdapter.IncidentClickListener {
            override fun onIncidentClick(incidentId: Long) {
                openIncidentDetails(incidentId)
            }
        })
        
        recyclerView.adapter = incidentAdapter
        
        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadIncidents()
        }
        
        // Set up FAB
        val fab: FloatingActionButton = findViewById(R.id.fab_add_incident)
        fab.setOnClickListener {
            val intent = Intent(this, ReportIncidentActivity::class.java)
            startActivity(intent)
        }
        
        // Load incidents
        loadIncidents()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload incidents when returning to this activity
        loadIncidents()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_incident_list, menu)
        
        val toggleItem = menu.findItem(R.id.action_toggle_view)
        if (showOnlyUserIncidents) {
            toggleItem.setTitle(R.string.view_all_reports)
        } else {
            toggleItem.setTitle(R.string.view_my_reports)
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_toggle_view -> {
                toggleIncidentView()
                true
            }
            R.id.action_refresh -> {
                swipeRefreshLayout.isRefreshing = true
                loadIncidents()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun loadIncidents() {
        val incidents = if (showOnlyUserIncidents) {
            val userId = userManager.getCurrentUserId()
            if (userId != -1L) {
                databaseHelper.getUserIncidents(userId)
            } else {
                ArrayList()
            }
        } else {
            databaseHelper.getAllIncidents()
        }
        
        // Update adapter
        incidentAdapter.updateIncidents(incidents)
        
        // Show empty view if needed
        if (incidents.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            if (showOnlyUserIncidents) {
                emptyView.text = getString(R.string.no_user_incidents)
            } else {
                emptyView.text = getString(R.string.no_incidents)
            }
        } else {
            emptyView.visibility = View.GONE
        }
        
        // Stop refresh animation
        swipeRefreshLayout.isRefreshing = false
    }
    
    private fun toggleIncidentView() {
        showOnlyUserIncidents = !showOnlyUserIncidents
        
        // Update title
        supportActionBar?.title = if (showOnlyUserIncidents) {
            getString(R.string.my_reports)
        } else {
            getString(R.string.community_reports)
        }
        
        // Update menu
        invalidateOptionsMenu()
        
        // Reload incidents
        loadIncidents()
    }
    
    private fun openIncidentDetails(incidentId: Long) {
        val intent = Intent(this, IncidentViewActivity::class.java).apply {
            putExtra("INCIDENT_ID", incidentId)
        }
        startActivity(intent)
    }
} 