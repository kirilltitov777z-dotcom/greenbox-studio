package com.greenbox.studio

import android.content.Context
import android.content.SharedPreferences

/** Simple shared-preferences backed settings used by the whole app. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences("greenbox_settings", Context.MODE_PRIVATE)
    }

    var theme: Int
        get() = sp.getInt("theme", 0)
        set(value) { sp.edit().putInt("theme", value).apply() }

    var imageQuality: Int
        get() = sp.getInt("quality", 92)
        set(value) { sp.edit().putInt("quality", value).apply() }

    var maxImageSize: Int
        get() = sp.getInt("max_size", 1600)
        set(value) { sp.edit().putInt("max_size", value).apply() }

    var brushColor: Int
        get() = sp.getInt("brush_color", -65536)
        set(value) { sp.edit().putInt("brush_color", value).apply() }

    var brushSize: Int
        get() = sp.getInt("brush_size", 12)
        set(value) { sp.edit().putInt("brush_size", value).apply() }

    var showGrid: Boolean
        get() = sp.getBoolean("show_grid", true)
        set(value) { sp.edit().putBoolean("show_grid", value).apply() }

    var vibration: Boolean
        get() = sp.getBoolean("vibration", true)
        set(value) { sp.edit().putBoolean("vibration", value).apply() }

    var watermark: Boolean
        get() = sp.getBoolean("watermark", false)
        set(value) { sp.edit().putBoolean("watermark", value).apply() }

    fun reset() {
        sp.edit().clear().apply()
    }
}
