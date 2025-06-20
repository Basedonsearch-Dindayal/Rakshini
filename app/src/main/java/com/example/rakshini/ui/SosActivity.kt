package com.example.rakshini.ui.sos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.rakshini.R

class SosActivity : AppCompatActivity() {

    companion object {
        private const val VIBRATE_PERMISSION_REQUEST = 1001
        private const val SMS_PERMISSION_REQUEST = 1002
        private const val NOTIFICATION_PERMISSION_REQUEST = 1003
        private const val EMERGENCY_CONTACT_KEY = "emergency_contact"
        private const val EMERGENCY_NUMBER = "1090"
        private const val TAG = "SosActivity"
        private const val CHANNEL_ID = "sos_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var emergencyContactInput: EditText
    private val sharedPreferences by lazy { getSharedPreferences("SosPrefs", MODE_PRIVATE) }
    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val sosButton = findViewById<Button>(R.id.sos_button)
        emergencyContactInput = findViewById(R.id.emergency_contact_input)
        val saveContactButton = findViewById<Button>(R.id.save_contact_button)

        // Load saved emergency contact
        val savedContact = sharedPreferences.getString(EMERGENCY_CONTACT_KEY, "")
        if (savedContact?.isNotEmpty() == true) {
            emergencyContactInput.setText(savedContact)
            Log.d(TAG, "Loaded emergency contact: $savedContact")
        }

        sosButton.setOnClickListener {
            if (checkPermissions()) {
                vibrateAndPlaySound()
                sendSosMessages()
                Toast.makeText(this, "SOS Alert Triggered!", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissions()
            }
        }

        saveContactButton.setOnClickListener {
            Log.d(TAG, "Save Contact button clicked")
            val contact = emergencyContactInput.text.toString().trim()
            Log.d(TAG, "Input contact: $contact")
            if (contact.isNotEmpty() && contact.length == 10 && contact.all { it.isDigit() }) {
                sharedPreferences.edit().putString(EMERGENCY_CONTACT_KEY, contact).apply()
                Log.d(TAG, "Contact saved: $contact")
                Toast.makeText(this, "Emergency contact saved: $contact", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Invalid contact: $contact")
                Toast.makeText(this, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SOS Alerts"
            val descriptionText = "Notifications for SOS emergency alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissions(): Boolean {
        val vibratePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED
        val smsPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below API 33
        }
        return vibratePermission && smsPermission && notificationPermission
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.VIBRATE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), VIBRATE_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == VIBRATE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                vibrateAndPlaySound()
                sendSosMessages()
                Toast.makeText(this, "SOS Alert Triggered!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied. SOS alert limited.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun vibrateAndPlaySound() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 200, 100, 200, 100, 200, 100, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        } else {
            Toast.makeText(this, "Device has no vibrator.", Toast.LENGTH_SHORT).show()
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.sos_sound)
        mediaPlayer?.setOnCompletionListener {
            mediaPlayer?.release()
            mediaPlayer = null
        }
        mediaPlayer?.start()
    }

    private fun sendSosMessages() {
        val smsManager = SmsManager.getDefault()
        val message = "Emergency SOS Alert from ${android.os.Build.MODEL} at ${System.currentTimeMillis()}!"
        var smsSentToBoth = false

        // Send to emergency number 1090
        try {
            smsManager.sendTextMessage(EMERGENCY_NUMBER, null, message, null, null)
            Toast.makeText(this, "SOS sent to 1090", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS to 1090: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Send to saved emergency contact
        val emergencyContact = sharedPreferences.getString(EMERGENCY_CONTACT_KEY, null)
        if (!emergencyContact.isNullOrEmpty() && emergencyContact.length == 10 && emergencyContact.all { it.isDigit() }) {
            try {
                smsManager.sendTextMessage(emergencyContact, null, message, null, null)
                Toast.makeText(this, "SOS sent to emergency contact", Toast.LENGTH_SHORT).show()
                smsSentToBoth = true
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send SMS to contact: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No valid emergency contact saved", Toast.LENGTH_SHORT).show()
        }

        // Show notification if SMS sent to both 1090 and emergency contact, and permission is granted
        if (smsSentToBoth && checkNotificationPermission()) {
            showNotification()
        } else if (smsSentToBoth && !checkNotificationPermission()) {
            Toast.makeText(this, "Notification permission required to confirm SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below API 33
        }
    }

    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with a custom icon if available
            .setContentTitle("SOS Alert")
            .setContentText("Emergency SOS message sent successfully to 1090 and your contact!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200)) // Short vibration pattern

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}