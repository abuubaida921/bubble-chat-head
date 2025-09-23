package com.abuubaida921.bubble_chat_head

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class MainActivity : AppCompatActivity() {
    private val overlayRequestLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permission after returning from settings
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, ChatHeadService::class.java))
        }
    }

    private val mediaProjectionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            if (mediaProjection != null) {
                startScreenSelection(mediaProjection)
            } else {
                Toast.makeText(this, "Failed to get MediaProjection", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var mediaProjectionHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val isScreenSelection = intent?.getBooleanExtra("START_SCREEN_SELECTION", false) == true
        if (isScreenSelection) {
            setTheme(R.style.Theme_Bubble_Chat_Head_Transparent)
        }
        super.onCreate(savedInstanceState)
        if (isScreenSelection) {
            window.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(R.layout.activity_transparent)
        } else {
            setContentView(R.layout.activity_main)
        }
        // Hide chat head overlay if launched for screen selection
        if (isScreenSelection) {
            sendBroadcast(Intent("com.abuubaida921.bubble_chat_head.HIDE_CHAT_HEAD"))
        }
        // Start foreground service for media projection before requesting permission
        ChatHeadService.startForegroundService(this)
        if (!isScreenSelection) {
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
            // Add a button or trigger to start screen capture
            // For demonstration, start on long press of the switch
            switch.setOnLongClickListener {
                requestScreenCapturePermission()
                true
            }
        }
        if (isScreenSelection) {
            requestScreenCapturePermission()
        }
    }

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionRequestLauncher.launch(intent)
    }

    private fun startScreenSelection(mediaProjection: MediaProjection) {
        // Overlay a transparent view for user to select region
        val overlay = SelectionOverlayView(this) { rect ->
            captureSelectedRegion(mediaProjection, rect)
        }
        addContentView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun captureSelectedRegion(mediaProjection: MediaProjection, rect: Rect) {
        // Register MediaProjection.Callback as required by Android 13+
        if (mediaProjectionCallback == null) {
            mediaProjectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    // Clean up if needed
                }
            }
        }
        if (mediaProjectionHandler == null) {
            mediaProjectionHandler = Handler(Looper.getMainLooper())
        }
        mediaProjection.registerCallback(mediaProjectionCallback!!, mediaProjectionHandler)
        // ...existing code for capture...
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                virtualDisplay?.release()
                imageReader.close()
                // Crop to selected region
                val cropped = Bitmap.createBitmap(
                    bitmap,
                    rect.left, rect.top,
                    rect.width(), rect.height()
                )
                runTextRecognition(cropped)
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                virtualDisplay?.release()
                imageReader.close()
            }
            // Unregister callback after capture
            mediaProjectionCallback?.let { mediaProjection.unregisterCallback(it) }
        }, 300)
        // After capture, stop foreground service
        ChatHeadService.stopForegroundService(this)
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Toast.makeText(this, "Extracted: $extractedText", Toast.LENGTH_LONG).show()
                // Show chat head again if launched for background selection
                if (intent?.getBooleanExtra("START_SCREEN_SELECTION", false) == true) {
                    sendBroadcast(Intent("com.abuubaida921.bubble_chat_head.SHOW_CHAT_HEAD"))
                    finish()
                }
                // TODO: Send extractedText to chat system
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                if (intent?.getBooleanExtra("START_SCREEN_SELECTION", false) == true) {
                    sendBroadcast(Intent("com.abuubaida921.bubble_chat_head.SHOW_CHAT_HEAD"))
                    finish()
                }
            }
    }

    private fun isServiceRunning(): Boolean {
        // TODO: Implement a check to see if ChatHeadService is running, for now return false
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("START_SCREEN_SELECTION", false)) {
            ChatHeadService.startForegroundService(this)
            requestScreenCapturePermission()
        }
    }
}

class SelectionOverlayView(context: Context, val onRegionSelected: (Rect) -> Unit) : View(context) {
    private var start: Point? = null
    private var end: Point? = null
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                performClick() // for accessibility
                start = Point(event.x.toInt(), event.y.toInt())
                end = null
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                end = Point(event.x.toInt(), event.y.toInt())
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                end = Point(event.x.toInt(), event.y.toInt())
                invalidate()
                val rect = getRect()
                if (rect != null) {
                    onRegionSelected(rect)
                    (parent as? ViewGroup)?.removeView(this)
                }
            }
        }
        return true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = getRect()
        if (rect != null) {
            canvas.drawRect(rect, paint)
        }
    }
    private fun getRect(): Rect? {
        val s = start
        val e = end
        return if (s != null && e != null) {
            Rect(
                Math.min(s.x, e.x),
                Math.min(s.y, e.y),
                Math.max(s.x, e.x),
                Math.max(s.y, e.y)
            )
        } else null
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}