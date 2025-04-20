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

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        nameLayout = findViewById(R.id.name_layout)
        emailLayout = findViewById(R.id.email_layout)
        passwordLayout = findViewById(R.id.password_layout)
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout)
        registerButton = findViewById(R.id.register_button)
        loginLink = findViewById(R.id.login_link)
        progressBar = findViewById(R.id.register_progress)

        // Initialize UserManager
        userManager = UserManager(this)

        // Set up register button click listener
        registerButton.setOnClickListener {
            registerUser()
        }

        // Set up login link click listener
        loginLink.setOnClickListener {
            // Navigate back to LoginActivity
            finish()
        }
    }

    private fun registerUser() {
        // Clear previous errors
        nameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null
        
        // Get input values
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        // Validate inputs
        var isValid = true

        if (name.isEmpty()) {
            nameLayout.error = "Name is required"
            nameInput.requestFocus()
            isValid = false
        }

        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            if (isValid) {
                emailInput.requestFocus()
            }
            isValid = false
        } else if (!userManager.isValidEmail(email)) {
            emailLayout.error = "Please enter a valid email address"
            if (isValid) {
                emailInput.requestFocus()
            }
            isValid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            if (isValid) {
                passwordInput.requestFocus()
            }
            isValid = false
        } else if (!userManager.isStrongPassword(password)) {
            passwordLayout.error = "Password must be at least 8 characters and include letters and numbers"
            if (isValid) {
                passwordInput.requestFocus()
            }
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your password"
            if (isValid) {
                confirmPasswordInput.requestFocus()
            }
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            if (isValid) {
                confirmPasswordInput.requestFocus()
            }
            isValid = false
        }
        
        if (!isValid) {
            return
        }

        // Show progress
        progressBar.visibility = View.VISIBLE
        registerButton.isEnabled = false

        // Attempt registration
        Handler(Looper.getMainLooper()).postDelayed({
            if (userManager.registerUser(name, email, password)) {
                // Registration successful
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration successful! Please login",
                    Toast.LENGTH_LONG
                ).show()

                // Navigate back to LoginActivity
                finish()
            } else {
                // Registration failed
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration failed. This email may already be registered.",
                    Toast.LENGTH_LONG
                ).show()
                progressBar.visibility = View.GONE
                registerButton.isEnabled = true
                
                // Show specific error on email field
                emailLayout.error = "This email is already registered"
            }
        }, 1000) // Simulated 1-second delay
    }
} 