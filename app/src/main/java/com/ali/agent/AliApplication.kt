package com.ali.agent

import android.app.Application
import com.ali.agent.data.db.AliDatabase

class AliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm on-device SQLite memory & knowledge database
        AliDatabase.getInstance(this)
    }
}
