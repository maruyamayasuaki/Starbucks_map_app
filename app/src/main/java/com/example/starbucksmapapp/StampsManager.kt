package com.example.starbucksmapapp

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class StampsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stamps_prefs", Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    fun addStamp(placeId: String, name: String) {
        val timestamp = dateFormat.format(Date())
        val stampData = "$name|$timestamp"
        prefs.edit().putString(placeId, stampData).apply()
    }

    fun hasStamp(placeId: String): Boolean {
        return prefs.contains(placeId)
    }

    fun getAllStamps(): List<StampInfo> {
        return prefs.all.map { entry ->
            val data = entry.value as String
            val parts = data.split("|")
            StampInfo(
                placeId = entry.key,
                name = if (parts.isNotEmpty()) parts[0] else "Unknown",
                timestamp = if (parts.size > 1) parts[1] else "Unknown"
            )
        }.sortedByDescending { it.timestamp }
    }

    fun getStampCount(): Int {
        return prefs.all.size
    }

    fun removeStamp(placeId: String) {
        prefs.edit().remove(placeId).apply()
    }

    fun clearAllStamps() {
        prefs.edit().clear().apply()
    }
}

data class StampInfo(
    val placeId: String,
    val name: String,
    val timestamp: String
)