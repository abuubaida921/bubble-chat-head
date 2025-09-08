package com.abuubaida921.bubble_chat_head

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings


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

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            overlayRequestLauncher.launch(intent)
        } else {
            startService(Intent(this, ChatHeadService::class.java))
        }
    }
}