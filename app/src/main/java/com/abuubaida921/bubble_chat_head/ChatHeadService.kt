package com.abuubaida921.bubble_chat_head

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView

class ChatHeadService : Service() {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
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

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        lastAction = event.action
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
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // Open the app when the chat head is clicked
                            val intent = Intent(this@ChatHeadService, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
}
