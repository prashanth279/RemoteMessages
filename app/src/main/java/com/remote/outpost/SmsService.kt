package com.remote.outpost

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class SmsService : Service() {
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                Telephony.Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->
                    val sender = messages[0].originatingAddress ?: "Unknown"
                    val body = messages.joinToString("") { it.messageBody ?: "" }
                    CoroutineScope(Dispatchers.IO).launch {
                        TelegramClient.sendMessageToTelegram(context, "From: $sender\n$body")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("sms_channel", "Outpost Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        val notification = NotificationCompat.Builder(this, "sms_channel")
            .setContentTitle("Outpost Active")
            .setContentText("Forwarding messages to Telegram")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        registerReceiver(smsReceiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION), RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { super.onDestroy(); unregisterReceiver(smsReceiver) }
    override fun onBind(intent: Intent?): IBinder? = null
}
