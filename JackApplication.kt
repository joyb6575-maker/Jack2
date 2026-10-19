package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.JackPreferences

class JackApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: JackPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        preferences = JackPreferences(this)
    }

    companion object {
        lateinit var instance: JackApplication
            private set
    }
}
