package com.example.appathon

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sqrt

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "appathon.db"
        private const val DATABASE_VERSION = 1

        // Users table
        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_NAME = "name"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_PASSWORD = "password"

        // Emergency contacts table
        const val TABLE_EMERGENCY_CONTACTS = "emergency_contacts"
        const val COLUMN_CONTACT_ID = "id"
        const val COLUMN_CONTACT_NAME = "name"
        const val COLUMN_CONTACT_PHONE = "phone"
        const val COLUMN_CONTACT_USER_ID = "user_id"
        const val COLUMN_CONTACT_RELATIONSHIP = "relationship"
        
        // Incidents table
        const val TABLE_INCIDENTS = "incidents"
        const val COLUMN_INCIDENT_ID = "id"
        const val COLUMN_INCIDENT_TITLE = "title"
        const val COLUMN_INCIDENT_TYPE = "type"
        const val COLUMN_INCIDENT_DESCRIPTION = "description"
        const val COLUMN_INCIDENT_LATITUDE = "latitude"
        const val COLUMN_INCIDENT_LONGITUDE = "longitude"
        const val COLUMN_INCIDENT_ADDRESS = "address"
        const val COLUMN_INCIDENT_PHOTO_PATH = "photo_path"
        const val COLUMN_INCIDENT_REPORTED_BY = "reported_by"
        const val COLUMN_INCIDENT_REPORTED_AT = "reported_at"
        const val COLUMN_INCIDENT_USER_ID = "user_id"
        
        // Safe Zones table
        const val TABLE_SAFE_ZONES = "safe_zones"
        const val COLUMN_SAFE_ZONE_ID = "id"
        const val COLUMN_SAFE_ZONE_NAME = "name"
        const val COLUMN_SAFE_ZONE_TYPE = "type"
        const val COLUMN_SAFE_ZONE_LATITUDE = "latitude"
        const val COLUMN_SAFE_ZONE_LONGITUDE = "longitude"
        const val COLUMN_SAFE_ZONE_ADDRESS = "address"
        const val COLUMN_SAFE_ZONE_CONTACT = "contact_number"
        const val COLUMN_SAFE_ZONE_HOURS = "operation_hours"
        const val COLUMN_SAFE_ZONE_ICON = "icon_res_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = "CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USER_NAME TEXT, " +
                "$COLUMN_USER_EMAIL TEXT UNIQUE, " +
                "$COLUMN_USER_PASSWORD TEXT" +
                ")"
        db.execSQL(createUsersTable)

        // Create emergency contacts table
        val createContactsTable = "CREATE TABLE $TABLE_EMERGENCY_CONTACTS (" +
                "$COLUMN_CONTACT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_CONTACT_NAME TEXT, " +
                "$COLUMN_CONTACT_PHONE TEXT, " +
                "$COLUMN_CONTACT_USER_ID INTEGER, " +
                "$COLUMN_CONTACT_RELATIONSHIP TEXT, " +
                "FOREIGN KEY($COLUMN_CONTACT_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)" +
                ")"
        db.execSQL(createContactsTable)
        
        // Create incidents table
        val createIncidentsTable = "CREATE TABLE $TABLE_INCIDENTS (" +
                "$COLUMN_INCIDENT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_INCIDENT_TITLE TEXT, " +
                "$COLUMN_INCIDENT_TYPE TEXT, " +
                "$COLUMN_INCIDENT_DESCRIPTION TEXT, " +
                "$COLUMN_INCIDENT_LATITUDE REAL, " +
                "$COLUMN_INCIDENT_LONGITUDE REAL, " +
                "$COLUMN_INCIDENT_ADDRESS TEXT, " +
                "$COLUMN_INCIDENT_PHOTO_PATH TEXT, " +
                "$COLUMN_INCIDENT_REPORTED_BY TEXT, " +
                "$COLUMN_INCIDENT_REPORTED_AT TEXT, " +
                "$COLUMN_INCIDENT_USER_ID INTEGER, " +
                "FOREIGN KEY($COLUMN_INCIDENT_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)" +
                ")"
        db.execSQL(createIncidentsTable)
        
        // Create safe zones table
        val createSafeZonesTable = "CREATE TABLE $TABLE_SAFE_ZONES (" +
                "$COLUMN_SAFE_ZONE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_SAFE_ZONE_NAME TEXT, " +
                "$COLUMN_SAFE_ZONE_TYPE TEXT, " +
                "$COLUMN_SAFE_ZONE_LATITUDE REAL, " +
                "$COLUMN_SAFE_ZONE_LONGITUDE REAL, " +
                "$COLUMN_SAFE_ZONE_ADDRESS TEXT, " +
                "$COLUMN_SAFE_ZONE_CONTACT TEXT, " +
                "$COLUMN_SAFE_ZONE_HOURS TEXT, " +
                "$COLUMN_SAFE_ZONE_ICON INTEGER" +
                ")"
        db.execSQL(createSafeZonesTable)
        
        // Populate with default safe zones
        insertDefaultSafeZones(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMERGENCY_CONTACTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INCIDENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")

        // Create tables again
        onCreate(db)
    }
    
    // Initialize with default safe zones
    private fun insertDefaultSafeZones(db: SQLiteDatabase) {
        // Default safe zones - In a real app you would populate this with real data
        val safeZones = listOf(
            arrayOf("Central Police Station", "POLICE", 37.798, -122.406, "766 Vallejo St, San Francisco", "415-315-2400", "24/7", R.drawable.ic_police),
            arrayOf("SF General Hospital", "HOSPITAL", 37.755, -122.406, "1001 Potrero Ave, San Francisco", "415-206-8000", "24/7", R.drawable.ic_hospital),
            arrayOf("City Fire Station #1", "FIRE_STATION", 37.779, -122.419, "935 Folsom St, San Francisco", "415-558-3200", "24/7", R.drawable.ic_fire_station),
            arrayOf("Downtown Police Station", "POLICE", 37.789, -122.401, "301 Eddy St, San Francisco", "415-345-7300", "24/7", R.drawable.ic_police),
            arrayOf("UC Medical Center", "HOSPITAL", 37.763, -122.458, "505 Parnassus Ave, San Francisco", "415-476-1000", "24/7", R.drawable.ic_hospital)
        )
        
        for (zone in safeZones) {
            val values = ContentValues().apply {
                put(COLUMN_SAFE_ZONE_NAME, zone[0] as String)
                put(COLUMN_SAFE_ZONE_TYPE, zone[1] as String)
                put(COLUMN_SAFE_ZONE_LATITUDE, zone[2] as Double)
                put(COLUMN_SAFE_ZONE_LONGITUDE, zone[3] as Double)
                put(COLUMN_SAFE_ZONE_ADDRESS, zone[4] as String)
                put(COLUMN_SAFE_ZONE_CONTACT, zone[5] as String)
                put(COLUMN_SAFE_ZONE_HOURS, zone[6] as String)
                put(COLUMN_SAFE_ZONE_ICON, zone[7] as Int)
            }
            db.insert(TABLE_SAFE_ZONES, null, values)
        }
    }

    // Safe Zones methods
    fun addSafeZone(zone: SafeZone): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SAFE_ZONE_NAME, zone.name)
            put(COLUMN_SAFE_ZONE_TYPE, zone.type)
            put(COLUMN_SAFE_ZONE_LATITUDE, zone.latitude)
            put(COLUMN_SAFE_ZONE_LONGITUDE, zone.longitude)
            put(COLUMN_SAFE_ZONE_ADDRESS, zone.address)
            put(COLUMN_SAFE_ZONE_CONTACT, zone.contactNumber)
            put(COLUMN_SAFE_ZONE_HOURS, zone.operationHours)
            put(COLUMN_SAFE_ZONE_ICON, zone.iconResId)
        }
        
        val id = db.insert(TABLE_SAFE_ZONES, null, values)
        db.close()
        return id
    }
    
    fun getAllSafeZones(): List<SafeZone> {
        val safeZones = mutableListOf<SafeZone>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_SAFE_ZONES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_SAFE_ZONE_NAME ASC"
        )
        
        with(cursor) {
            while (moveToNext()) {
                val zone = SafeZone(
                    id = getInt(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_ID)),
                    name = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_NAME)),
                    type = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_TYPE)),
                    latitude = getDouble(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_LATITUDE)),
                    longitude = getDouble(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_LONGITUDE)),
                    address = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_ADDRESS)),
                    contactNumber = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_CONTACT)),
                    operationHours = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_HOURS)),
                    iconResId = getInt(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_ICON))
                )
                safeZones.add(zone)
            }
        }
        cursor.close()
        return safeZones
    }
    
    fun getSafeZonesByType(type: String): List<SafeZone> {
        val safeZones = mutableListOf<SafeZone>()
        val db = this.readableDatabase
        val selection = "$COLUMN_SAFE_ZONE_TYPE = ?"
        val selectionArgs = arrayOf(type)
        
        val cursor = db.query(
            TABLE_SAFE_ZONES,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_SAFE_ZONE_NAME ASC"
        )
        
        with(cursor) {
            while (moveToNext()) {
                val zone = SafeZone(
                    id = getInt(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_ID)),
                    name = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_NAME)),
                    type = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_TYPE)),
                    latitude = getDouble(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_LATITUDE)),
                    longitude = getDouble(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_LONGITUDE)),
                    address = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_ADDRESS)),
                    contactNumber = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_CONTACT)),
                    operationHours = getString(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_HOURS)),
                    iconResId = getInt(getColumnIndexOrThrow(COLUMN_SAFE_ZONE_ICON))
                )
                safeZones.add(zone)
            }
        }
        cursor.close()
        return safeZones
    }
    
    fun getNearestSafeZones(latitude: Double, longitude: Double, radius: Double, limit: Int = 5): List<SafeZone> {
        val allZones = getAllSafeZones()
        
        // Calculate distance for each zone
        val zonesWithDistance = allZones.map { zone ->
            val distance = calculateDistance(latitude, longitude, zone.latitude, zone.longitude)
            Pair(zone, distance)
        }
        
        // Filter zones within radius
        val filteredZones = zonesWithDistance.filter { it.second <= radius }
        
        // Sort by distance and take the specified limit
        return filteredZones.sortedBy { it.second }.take(limit).map { it.first }
    }
    
    // Helper method to calculate approximate distance between two points in kilometers
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Simple Euclidean distance calculation (approximation for small distances)
        // For more accuracy, you would use Haversine formula
        val latDiff = lat1 - lat2
        val lonDiff = lon1 - lon2 * cos(Math.toRadians(lat1))
        val distance = sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111.32 // 1 degree is approximately 111.32 km
        return distance
    }

    // User related methods
    fun addUser(name: String, email: String, password: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USER_NAME, name)
        values.put(COLUMN_USER_EMAIL, email)
        values.put(COLUMN_USER_PASSWORD, password)

        // Insert row
        val id = db.insert(TABLE_USERS, null, values)
        db.close()
        return id
    }

    fun getUserByEmail(email: String): Cursor? {
        val db = this.readableDatabase
        val selection = "$COLUMN_USER_EMAIL = ?"
        val selectionArgs = arrayOf(email)
        
        return db.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
    }

    fun getUserById(id: Long): Cursor? {
        val db = this.readableDatabase
        val selection = "$COLUMN_USER_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        
        return db.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
    }

    // Emergency contact methods
    fun addEmergencyContact(name: String, phone: String, userId: Long, relationship: String = ""): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_CONTACT_NAME, name)
        values.put(COLUMN_CONTACT_PHONE, phone)
        values.put(COLUMN_CONTACT_USER_ID, userId)
        values.put(COLUMN_CONTACT_RELATIONSHIP, relationship)

        // Insert row
        val id = db.insert(TABLE_EMERGENCY_CONTACTS, null, values)
        db.close()
        return id
    }

    fun getEmergencyContacts(userId: Long): Cursor {
        val db = this.readableDatabase
        val selection = "$COLUMN_CONTACT_USER_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        
        return db.query(
            TABLE_EMERGENCY_CONTACTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
    }

    fun updateEmergencyContact(id: Long, name: String, phone: String, relationship: String = ""): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_CONTACT_NAME, name)
        values.put(COLUMN_CONTACT_PHONE, phone)
        values.put(COLUMN_CONTACT_RELATIONSHIP, relationship)

        // Update row
        val result = db.update(
            TABLE_EMERGENCY_CONTACTS,
            values,
            "$COLUMN_CONTACT_ID = ?",
            arrayOf(id.toString())
        )
        db.close()
        return result
    }

    fun deleteEmergencyContact(id: Long): Int {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_EMERGENCY_CONTACTS,
            "$COLUMN_CONTACT_ID = ?",
            arrayOf(id.toString())
        )
        db.close()
        return result
    }

    fun getEmergencyContactCount(userId: Long): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EMERGENCY_CONTACTS WHERE $COLUMN_CONTACT_USER_ID = ?",
            arrayOf(userId.toString())
        )
        
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }
    
    // Incident methods
    fun addIncident(
        title: String,
        type: String,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        photoPath: String?,
        reportedBy: String,
        userId: Long
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_INCIDENT_TITLE, title)
        values.put(COLUMN_INCIDENT_TYPE, type)
        values.put(COLUMN_INCIDENT_DESCRIPTION, description)
        values.put(COLUMN_INCIDENT_LATITUDE, latitude)
        values.put(COLUMN_INCIDENT_LONGITUDE, longitude)
        values.put(COLUMN_INCIDENT_ADDRESS, address)
        values.put(COLUMN_INCIDENT_PHOTO_PATH, photoPath)
        values.put(COLUMN_INCIDENT_REPORTED_BY, reportedBy)
        values.put(COLUMN_INCIDENT_REPORTED_AT, formatDate(Date()))
        values.put(COLUMN_INCIDENT_USER_ID, userId)
        
        // Insert row
        val id = db.insert(TABLE_INCIDENTS, null, values)
        db.close()
        return id
    }
    
    fun getAllIncidents(): List<Incident> {
        val incidents = ArrayList<Incident>()
        val cursor = getAllIncidentsCursor()
        
        if (cursor.moveToFirst()) {
            do {
                incidents.add(cursorToIncident(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        return incidents
    }
    
    private fun getAllIncidentsCursor(): Cursor {
        val db = this.readableDatabase
        return db.query(
            TABLE_INCIDENTS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_INCIDENT_REPORTED_AT DESC" // Most recent first
        )
    }
    
    fun getIncidentById(id: Long): Incident? {
        val db = this.readableDatabase
        val selection = "$COLUMN_INCIDENT_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        
        val cursor = db.query(
            TABLE_INCIDENTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        var incident: Incident? = null
        if (cursor.moveToFirst()) {
            incident = cursorToIncident(cursor)
        }
        cursor.close()
        
        return incident
    }
    
    fun getNearbyIncidents(latitude: Double, longitude: Double, radiusKm: Double): List<Incident> {
        val incidents = ArrayList<Incident>()
        val cursor = getNearbyIncidentsCursor(latitude, longitude, radiusKm)
        
        if (cursor.moveToFirst()) {
            do {
                incidents.add(cursorToIncident(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        return incidents
    }
    
    private fun getNearbyIncidentsCursor(latitude: Double, longitude: Double, radiusKm: Double): Cursor {
        // This is a simple approximation, for a real app you'd use proper geospatial queries
        // 1 degree of latitude = ~111km, 1 degree of longitude varies by latitude
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)))
        
        val minLat = latitude - latDelta
        val maxLat = latitude + latDelta
        val minLon = longitude - lonDelta
        val maxLon = longitude + lonDelta
        
        val db = this.readableDatabase
        val selection = "$COLUMN_INCIDENT_LATITUDE BETWEEN ? AND ? AND " +
                "$COLUMN_INCIDENT_LONGITUDE BETWEEN ? AND ?"
        val selectionArgs = arrayOf(
            minLat.toString(),
            maxLat.toString(),
            minLon.toString(),
            maxLon.toString()
        )
        
        return db.query(
            TABLE_INCIDENTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_INCIDENT_REPORTED_AT DESC" // Most recent first
        )
    }
    
    fun deleteIncident(id: Long): Int {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_INCIDENTS,
            "$COLUMN_INCIDENT_ID = ?",
            arrayOf(id.toString())
        )
        db.close()
        return result
    }
    
    fun getUserIncidents(userId: Long): List<Incident> {
        val incidents = ArrayList<Incident>()
        val cursor = getUserIncidentsCursor(userId)
        
        if (cursor.moveToFirst()) {
            do {
                incidents.add(cursorToIncident(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        return incidents
    }
    
    private fun getUserIncidentsCursor(userId: Long): Cursor {
        val db = this.readableDatabase
        val selection = "$COLUMN_INCIDENT_USER_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        
        return db.query(
            TABLE_INCIDENTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_INCIDENT_REPORTED_AT DESC" // Most recent first
        )
    }
    
    private fun cursorToIncident(cursor: Cursor): Incident {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_TITLE))
        val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_TYPE))
        val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_DESCRIPTION))
        val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_LONGITUDE))
        val address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_ADDRESS))
        val photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_PHOTO_PATH))
        val reportedBy = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_REPORTED_BY))
        val reportedAtStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_REPORTED_AT))
        val userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INCIDENT_USER_ID))
        
        return Incident(
            id,
            title,
            type,
            description,
            latitude,
            longitude,
            address,
            photoPath,
            reportedBy,
            parseDate(reportedAtStr),
            userId
        )
    }
    
    // Helper method to format date for SQLite
    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(date)
    }
    
    // Helper method to parse date from SQLite
    fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            dateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
} 