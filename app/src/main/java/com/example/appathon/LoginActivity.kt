package com.example.appathon

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        emailLayout = findViewById(R.id.email_layout)
        passwordLayout = findViewById(R.id.password_layout)
        loginButton = findViewById(R.id.login_button)
        registerLink = findViewById(R.id.register_link)
        progressBar = findViewById(R.id.login_progress)

        // Initialize UserManager
        userManager = UserManager(this)

        // Check if user is already logged in
        if (userManager.isLoggedIn()) {
            // User is already logged in, go to MainActivity
            startMainActivity()
            finish()
            return
        }

        // Set up login button click listener
        loginButton.setOnClickListener {
            loginUser()
        }

        // Set up register link click listener
        registerLink.setOnClickListener {
            // Navigate to RegisterActivity
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        // Clear previous errors
        emailLayout.error = null
        passwordLayout.error = null
        
        // Get input values
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Validate inputs
        var isValid = true
        
        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            emailInput.requestFocus()
            isValid = false
        } else if (!userManager.isValidEmail(email)) {
            emailLayout.error = "Please enter a valid email address"
            emailInput.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            if (isValid) {
                passwordInput.requestFocus()
            }
            isValid = false
        }
        
        if (!isValid) {
            return
        }

        // Show progress
        progressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false

        // Attempt login
        Handler(Looper.getMainLooper()).postDelayed({
            if (userManager.loginUser(email, password)) {
                // Login successful
                Toast.makeText(
                    this@LoginActivity,
                    "Login successful",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate to MainActivity
                startMainActivity()
                finish()
            } else {
                // Login failed
                Toast.makeText(
                    this@LoginActivity,
                    "Invalid email or password",
                    Toast.LENGTH_LONG
                ).show()
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
                
                // Show specific error on password field
                passwordLayout.error = "Invalid email or password"
            }
        }, 1000) // Simulated 1-second delay
    }

    private fun startMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
    }
} 