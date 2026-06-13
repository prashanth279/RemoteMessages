package com.remote.outpost

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.remote.outpost.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            startSmsService()
        } else {
            Toast.makeText(this, "Permissions required for background operation", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkInitialPermissions()
    }

    private fun setupUI() {
        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener

            if (!hasAllPermissions()) {
                requestPermissionLauncher.launch(requiredPermissions)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                binding.sendButton.isEnabled = false
                try {
                    val response = withContext(Dispatchers.IO) {
                        TelegramClient.sendMessageToTelegram(this@MainActivity, message)
                    }
                    Toast.makeText(this@MainActivity, response, Toast.LENGTH_SHORT).show()
                    if (response.contains("sent", ignoreCase = true)) {
                        binding.messageEditText.text?.clear()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.sendButton.isEnabled = true
                }
            }
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.permissionsButton.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
    }

    private fun checkInitialPermissions() {
        if (hasAllPermissions()) {
            startSmsService()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startSmsService() {
        val intent = Intent(this, SmsService::class.java)
        startForegroundService(intent)
    }
}
