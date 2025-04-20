package com.example.appathon

import android.content.Context
import android.content.SharedPreferences
import java.util.regex.Pattern

/**
 * Manages user authentication and session information
 */
class UserManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    private val dbHelper = DatabaseHelper(context)
    
    /**
     * Register a new user
     * @return True if registration successful, false otherwise
     */
    fun registerUser(name: String, email: String, password: String): Boolean {
        // Validate email format
        if (!isValidEmail(email)) {
            return false
        }
        
        // Validate password strength
        if (!isStrongPassword(password)) {
            return false
        }
        
        // Check if user already exists
        val cursor = dbHelper.getUserByEmail(email)
        if (cursor != null && cursor.count > 0) {
            cursor.close()
            return false
        }
        cursor?.close()
        
        // Add user to database
        val userId = dbHelper.addUser(name, email, password)
        return userId != -1L
    }
    
    /**
     * Login a user
     * @return True if login successful, false otherwise
     */
    fun loginUser(email: String, password: String): Boolean {
        // Query database for user
        val cursor = dbHelper.getUserByEmail(email)
        
        if (cursor != null && cursor.moveToFirst()) {
            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PASSWORD))
            val userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME))
            
            cursor.close()
            
            if (storedPassword == password) {
                // Login successful, save current user
                saveCurrentUser(userId, email, name)
                return true
            }
        }
        cursor?.close()
        
        return false
    }
    
    /**
     * Save current user session
     */
    private fun saveCurrentUser(userId: Long, email: String, name: String) {
        val editor = sharedPreferences.edit()
        editor.putLong(KEY_CURRENT_USER_ID, userId)
        editor.putString(KEY_CURRENT_USER_EMAIL, email)
        editor.putString(KEY_CURRENT_USER_NAME, name)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Get current user name
     */
    fun getCurrentUserName(): String {
        return sharedPreferences.getString(KEY_CURRENT_USER_NAME, "") ?: ""
    }
    
    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String {
        return sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, "") ?: ""
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): Long {
        return sharedPreferences.getLong(KEY_CURRENT_USER_ID, -1)
    }
    
    /**
     * Logout current user
     */
    fun logoutUser() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_CURRENT_USER_ID)
        editor.remove(KEY_CURRENT_USER_EMAIL)
        editor.remove(KEY_CURRENT_USER_NAME)
        editor.putBoolean(KEY_IS_LOGGED_IN, false)
        editor.apply()
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }
    
    /**
     * Validate password strength
     * - At least 8 characters
     * - Contains at least one digit
     * - Contains at least one letter
     */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) {
            return false
        }
        
        val hasLetter = Pattern.compile("[a-zA-Z]").matcher(password).find()
        val hasDigit = Pattern.compile("\\d").matcher(password).find()
        
        return hasLetter && hasDigit
    }
    
    /**
     * Logs out the current user
     */
    fun logout() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    /**
     * Gets the current logged in user
     */
    fun getCurrentUser(): User? {
        val userId = sharedPreferences.getString("userId", null) ?: return null
        val username = sharedPreferences.getString("username", null) ?: return null
        val email = sharedPreferences.getString("email", null) ?: return null
        val phone = sharedPreferences.getString("phone", null) ?: ""
        
        return User(userId, username, email, phone)
    }
    
    companion object {
        private const val PREF_NAME = "user_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_CURRENT_USER_NAME = "current_user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        
        @Volatile
        private var INSTANCE: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
} 