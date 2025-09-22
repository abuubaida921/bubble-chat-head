package com.abuubaida921.bubble_chat_head

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

class BubbleChatHeadApp : Application() {
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters foreground
                    val intent = Intent("com.abuubaida921.bubble_chat_head.HIDE_CHAT_HEAD")
                    sendBroadcast(intent)
                }
                activityReferences++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                activityReferences--
                if (activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters background
                    val intent = Intent("com.abuubaida921.bubble_chat_head.SHOW_CHAT_HEAD")
                    sendBroadcast(intent)
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}

