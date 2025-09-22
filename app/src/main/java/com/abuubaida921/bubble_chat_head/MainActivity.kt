package com.abuubaida921.bubble_chat_head

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import androidx.appcompat.widget.SwitchCompat
import android.content.SharedPreferences
import android.preference.PreferenceManager


class MainActivity : AppCompatActivity() {
    private val overlayRequestLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permission after returning from settings
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, ChatHeadService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("bubble_chat_head_prefs", MODE_PRIVATE)
        val switch = findViewById<SwitchCompat>(R.id.switch_floating_head)
        val isEnabled = prefs.getBoolean("show_floating_head", false)
        switch.isChecked = isEnabled
        if (isEnabled) {
            startService(Intent(this, ChatHeadService::class.java))
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_floating_head", isChecked).apply()
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                    overlayRequestLauncher.launch(intent)
                } else {
                    startService(Intent(this, ChatHeadService::class.java))
                }
            } else {
                stopService(Intent(this, ChatHeadService::class.java))
            }
        }
    }


    private fun isServiceRunning(): Boolean {
        // TODO: Implement a check to see if ChatHeadService is running, for now return false
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Optionally bring the app to the foreground if needed
    }
}