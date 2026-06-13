package com.remote.outpost

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object TelegramClient {
    private const val PREFS_NAME = "TelegramPrefs"
    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"
    private const val KEY_SAVED_PROFILES = "saved_profiles"
    
    private val client = OkHttpClient()

    /**
     * Returns the currently active configuration.
     * No hardcoded defaults - returns nulls if nothing is established manually.
     */
    fun getActiveConfig(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_BOT_TOKEN, null)
        val chatId = prefs.getString(KEY_CHAT_ID, null)
        return Pair(token, chatId)
    }

    /**
     * Saves a configuration as active and adds it to the history list if unique.
     */
    fun saveAndSetActiveConfig(context: Context, token: String, chatId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Set as the current active profile
        editor.putString(KEY_BOT_TOKEN, token)
        editor.putString(KEY_CHAT_ID, chatId)
        
        // Update the history list
        val profilesJson = prefs.getString(KEY_SAVED_PROFILES, "[]")
        val array = JSONArray(profilesJson)
        
        // Check if this configuration already exists in history
        var exists = false
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString(KEY_BOT_TOKEN) == token && obj.getString(KEY_CHAT_ID) == chatId) {
                exists = true
                break
            }
        }
        
        if (!exists) {
            val newProfile = JSONObject().apply {
                put(KEY_BOT_TOKEN, token)
                put(KEY_CHAT_ID, chatId)
                // Extract a display label from the token (prefix before colon or last 5 chars)
                val labelPrefix = if (token.contains(":")) token.split(":")[0].takeLast(5) else token.takeLast(5)
                put("label", "Bot ...$labelPrefix / ID: $chatId")
            }
            array.put(newProfile)
            editor.putString(KEY_SAVED_PROFILES, array.toString())
        }
        
        editor.apply()
    }

    /**
     * Returns all saved profiles from history.
     */
    fun getSavedProfiles(context: Context): List<Map<String, String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profilesJson = prefs.getString(KEY_SAVED_PROFILES, "[]")
        val array = JSONArray(profilesJson)
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(mapOf(
                "token" to obj.getString(KEY_BOT_TOKEN),
                "chatId" to obj.getString(KEY_CHAT_ID),
                "label" to obj.optString("label", "Profile $i")
            ))
        }
        return list.reversed() // Most recent first
    }

    /**
     * Sends a message to Telegram using the active configuration.
     */
    suspend fun sendMessageToTelegram(context: Context, message: String): String {
        val (botToken, chatId) = getActiveConfig(context)
        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            return "Error: Telegram not configured"
        }

        return try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=${URLEncoder.encode(message, "UTF-8")}"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) "Success" else "Error: ${response.code}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
