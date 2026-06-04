package com.bugra.campussync

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class CampusSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
