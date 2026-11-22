package com.greenbox.studio

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class ReactionActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var bestText: TextView
    private lateinit var target: Button
    private lateinit var start: Button
    private val handler = Handler(Looper.getMainLooper())
    private var waiting = false
    private var startTime = 0L
    private var best = Long.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Реакция")

        val p = dp(16)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        status = TextView(this).apply {
            text = "Нажмите «Старт» и ждите сигнала"
            textSize = 18f
            setTextColor(Color.parseColor("#2E7D32"))
        }
        root.addView(status)
        bestText = TextView(this).apply {
            text = "Лучший результат: —"
            textSize = 14f
            setTextColor(Color.parseColor("#6B7F6B"))
            setPadding(0, dp(6), 0, dp(20))
        }
        root.addView(bestText)

        target = Button(this).apply {
            text = "ЖДИ..."
            textSize = 28f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#9E9E9E"))
            isEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setOnClickListener { onTarget() }
        }
        root.addView(target)

        start = Button(this).apply {
            text = "Старт"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = p }
            setOnClickListener { startRound() }
        }
        root.addView(start)

        setContentView(root)
    }

    private fun startRound() {
        handler.removeCallbacksAndMessages(null)
        waiting = false
        start.isEnabled = false
        target.isEnabled = false
        target.text = "ЖДИ..."
        target.setBackgroundColor(Color.parseColor("#9E9E9E"))
        status.text = "Ждите зелёного сигнала..."
        val delay = 1500L + Random.nextInt(2500)
        handler.postDelayed(Runnable {
            waiting = true
            startTime = System.nanoTime()
            target.text = "ЖМИ!"
            target.setBackgroundColor(Color.parseColor("#43A047"))
            target.isEnabled = true
            status.text = "ЖМИ!"
            vibrate(this, 60)
        }, delay)
    }

    private fun onTarget() {
        if (!waiting) return
        val elapsed = ((System.nanoTime() - startTime) / 1_000_000f).toLong()
        waiting = false
        target.isEnabled = false
        target.setBackgroundColor(Color.parseColor("#9E9E9E"))
        target.text = "Твой ход!"
        status.text = "Реакция: $elapsed мс"
        if (elapsed < best) {
            best = elapsed
            bestText.text = "Лучший результат: $best мс"
        }
        start.isEnabled = true
    }
}
