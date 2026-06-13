package com.remote.outpost

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var botTokenEditText: EditText
    private lateinit var chatIdEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var profilesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        botTokenEditText = findViewById(R.id.botTokenEditText)
        chatIdEditText = findViewById(R.id.chatIdEditText)
        saveButton = findViewById(R.id.saveButton)
        profilesContainer = findViewById(R.id.profilesContainer)

        loadActiveConfig()
        refreshProfilesList()

        saveButton.setOnClickListener {
            val token = botTokenEditText.text.toString().trim()
            val chatId = chatIdEditText.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Please enter both Token and Chat ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            TelegramClient.saveAndSetActiveConfig(this, token, chatId)
            Toast.makeText(this, "Profile Saved and Activated", Toast.LENGTH_SHORT).show()
            
            sendConnectionTest("Outpost: Bot Connected on ${Build.MODEL}")
            refreshProfilesList()
        }

        // Automatic connection test if established
        val (token, id) = TelegramClient.getActiveConfig(this)
        if (!token.isNullOrEmpty() && !id.isNullOrEmpty()) {
            sendConnectionTest("Outpost Settings Opened - Connected")
        }
    }

    private fun loadActiveConfig() {
        val (token, chatId) = TelegramClient.getActiveConfig(this)
        botTokenEditText.setText(token ?: "")
        chatIdEditText.setText(chatId ?: "")
    }

    private fun sendConnectionTest(msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = TelegramClient.sendMessageToTelegram(this@SettingsActivity, msg)
            withContext(Dispatchers.Main) {
                if (result == "Success") {
                    Toast.makeText(this@SettingsActivity, "Connected to Telegram Bot", Toast.LENGTH_SHORT).show()
                } else if (result.contains("Error")) {
                    Toast.makeText(this@SettingsActivity, result, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun refreshProfilesList() {
        profilesContainer.removeAllViews()
        val profiles = TelegramClient.getSavedProfiles(this)
        
        if (profiles.isEmpty()) {
            findViewById<View>(R.id.historyTitle)?.visibility = View.GONE
            return
        }
        findViewById<View>(R.id.historyTitle)?.visibility = View.VISIBLE

        profiles.forEach { profile ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, (16 * resources.displayMetrics.density).toInt()) }
                radius = 16f * resources.displayMetrics.density
                cardElevation = 0f
                setCardBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.outpost_bg))
                strokeWidth = 0
                
                val inner = LinearLayout(this@SettingsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        (20 * resources.displayMetrics.density).toInt(),
                        (16 * resources.displayMetrics.density).toInt(),
                        (20 * resources.displayMetrics.density).toInt(),
                        (16 * resources.displayMetrics.density).toInt()
                    )
                    
                    addView(TextView(this@SettingsActivity).apply {
                        text = profile["label"]
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.outpost_dark))
                        typeface = Typeface.DEFAULT_BOLD
                    })
                    
                    addView(TextView(this@SettingsActivity).apply {
                        text = "ID: ${profile["chatId"]}"
                        textSize = 12f
                        setTextColor(android.graphics.Color.GRAY)
                    })
                }
                
                addView(inner)
                setOnClickListener {
                    botTokenEditText.setText(profile["token"])
                    chatIdEditText.setText(profile["chatId"])
                    saveButton.performClick()
                }
            }
            profilesContainer.addView(card)
        }
    }
}
