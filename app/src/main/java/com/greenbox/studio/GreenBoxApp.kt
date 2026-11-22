package com.greenbox.studio

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GreenBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        applyTheme()
    }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(when (Prefs.theme) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }
}
