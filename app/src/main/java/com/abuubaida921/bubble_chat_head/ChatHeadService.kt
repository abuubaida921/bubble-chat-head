package com.abuubaida921.bubble_chat_head

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class ChatHeadService : Service() {
    companion object {
        const val CHANNEL_ID = "chat_head_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_FOREGROUND = "START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "STOP_FOREGROUND"
        fun startForegroundService(context: Context) {
            val intent = Intent(context, ChatHeadService::class.java)
            intent.action = ACTION_START_FOREGROUND
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        fun stopForegroundService(context: Context) {
            val intent = Intent(context, ChatHeadService::class.java)
            intent.action = ACTION_STOP_FOREGROUND
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var chatHead: ImageView

    private val chatHeadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.abuubaida921.bubble_chat_head.HIDE_CHAT_HEAD" -> chatHead.visibility = View.GONE
                "com.abuubaida921.bubble_chat_head.SHOW_CHAT_HEAD" -> chatHead.visibility = View.VISIBLE
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        chatHead = ImageView(this).apply {
            setImageResource(R.drawable.ic_chat_head) // your bubble icon
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        // Dragging logic
        chatHead.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction = 0
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var longPressStartTime = 0L
            private val LONG_PRESS_THRESHOLD = 600L // ms

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v?.performClick()
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        lastAction = event.action
                        longPressStartTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(chatHead, params)
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val pressDuration = System.currentTimeMillis() - longPressStartTime
                        if (pressDuration >= LONG_PRESS_THRESHOLD) {
                            // Long press detected: start screen selection
                            val intent = Intent(this@ChatHeadService, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            intent.putExtra("START_SCREEN_SELECTION", true)
                            // TODO: For best reliability, consider starting this service as a foreground service before launching the intent
                            startActivity(intent)
                        } else if (lastAction == MotionEvent.ACTION_DOWN) {
                            // Short tap: open the app
                            val intent = Intent(this@ChatHeadService, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                        }
                        lastAction = event.action
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(chatHead, params)

        val filter = IntentFilter().apply {
            addAction("com.abuubaida921.bubble_chat_head.HIDE_CHAT_HEAD")
            addAction("com.abuubaida921.bubble_chat_head.SHOW_CHAT_HEAD")
        }
        registerReceiver(chatHeadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::chatHead.isInitialized) windowManager.removeView(chatHead)
        unregisterReceiver(chatHeadReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                startForegroundServiceWithNotification()
            }
            ACTION_STOP_FOREGROUND -> {
                stopForeground(true)
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Active")
            .setContentText("Bubble Chat Head is ready to capture your screen.")
            .setSmallIcon(R.drawable.ic_chat_head)
            .setContentIntent(pendingIntent)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
