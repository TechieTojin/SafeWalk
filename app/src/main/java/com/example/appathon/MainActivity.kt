package com.example.appathon

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.example.appathon.HomeFragment
import android.widget.Toast

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize UserManager
        userManager = UserManager(this)

        // Check if user is logged in
        if (!userManager.isLoggedIn()) {
            // User not logged in, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "SafeWalk"

        // Setup navigation drawer
        navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set user info in navigation drawer header
        val headerView = navigationView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.user_name)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.user_email)
        
        userNameTextView.text = userManager.getCurrentUserName()
        userEmailTextView.text = userManager.getCurrentUserEmail()

        // Add logout option to menu
        navigationView.menu.add(0, MENU_LOGOUT, 100, "Logout")
            .setIcon(android.R.drawable.ic_lock_power_off)

        // Setup bottom navigation
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance())
            bottomNavigation.selectedItemId = R.id.navigation_home
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
        return true
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportActionBar?.title = "SafeWalk"
                    loadFragment(HomeFragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_sos -> {
                    supportActionBar?.title = "SOS"
                    loadFragment(Page2Fragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_safety -> {
                    supportActionBar?.title = "Safety"
                    loadFragment(Page3Fragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_community -> {
                    supportActionBar?.title = "Community"
                    loadFragment(Page4Fragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_settings -> {
                    supportActionBar?.title = "Settings"
                    loadFragment(SettingsFragment.newInstance())
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
        
        // Default to home fragment
        bottomNavigation.selectedItemId = R.id.navigation_home
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                supportActionBar?.title = "SafeWalk"
                loadFragment(HomeFragment.newInstance())
            }
            R.id.nav_sos -> {
                supportActionBar?.title = "SOS"
                loadFragment(Page2Fragment.newInstance())
            }
            R.id.nav_safety_features -> {
                supportActionBar?.title = "Safety"
                loadFragment(Page3Fragment.newInstance())
            }
            R.id.nav_community_reports -> {
                supportActionBar?.title = "Community"
                loadFragment(Page4Fragment.newInstance())
            }
            R.id.nav_settings -> {
                supportActionBar?.title = "Settings"
                loadFragment(SettingsFragment.newInstance())
            }
            R.id.nav_share -> {
                Toast.makeText(this, "Share", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                userManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            MENU_LOGOUT -> {
                userManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // If not on home screen, go to home screen on back press
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is HomeFragment) {
                loadFragment(HomeFragment.newInstance())
                bottomNavigation.selectedItemId = R.id.navigation_home
            } else {
                super.onBackPressed()
            }
        }
    }
    
    companion object {
        private const val MENU_LOGOUT = 1001
    }
}