package com.jaytt.moveandmeds

import android.app.Application
import com.jaytt.moveandmeds.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
