package com.bugra.campussync

import android.app.Application
import com.bugra.campussync.network.RetrofitClient

class CampusSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
