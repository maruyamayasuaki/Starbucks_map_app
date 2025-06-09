package com.example.starbucksmapapp

import android.content.Context
import android.content.SharedPreferences

class StampsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stamps_prefs", Context.MODE_PRIVATE)

    fun addStamp(placeId: String, name: String) {
        prefs.edit().putString(placeId, name).apply()
    }

    fun getAllStamps(): Map<String, String> {
        return prefs.all.mapValues { it.value as String }
    }
}
