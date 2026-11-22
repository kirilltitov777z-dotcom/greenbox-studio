package com.greenbox.studio

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue

fun dp(value: Int): Int = Math.round(TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), Resources.getSystem().displayMetrics))

fun vibrate(context: Context, ms: Long = 30L) {
    if (!Prefs.vibration) return
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        v.vibrate(ms)
    }
}
