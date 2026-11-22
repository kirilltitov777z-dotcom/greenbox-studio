package com.greenbox.studio

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class DiceActivity : AppCompatActivity() {

    private val faces = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
    private val handler = Handler(Looper.getMainLooper())
    private var count = 2
    private var rolling = false
    private lateinit var diceRow: LinearLayout
    private lateinit var diceViews: Array<TextView>
    private lateinit var sumText: TextView
    private lateinit var rollButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Кубики")

        val p = dp(14)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        root.addView(TextView(this).apply {
            text = "Сколько кубиков бросить?"
            textSize = 16f
            setTextColor(Color.parseColor("#2E7D32"))
        })

        val picker = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (n in 1..6) {
            val num = n
            picker.addView(Button(this).apply {
                text = num.toString()
                setBackgroundResource(R.drawable.btn_soft)
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    setMargins(dp(2), dp(8), dp(2), 0)
                }
                setOnClickListener { count = num; buildDice() }
            })
        }
        root.addView(picker)

        diceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
            ).apply { topMargin = dp(12) }
        }
        root.addView(diceRow)

        sumText = TextView(this).apply {
            text = "Сумма: —"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1B2B1B"))
        }
        root.addView(sumText)

        rollButton = Button(this).apply {
            text = "🎲 Бросить"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = p }
            setOnClickListener { roll() }
        }
        root.addView(rollButton)

        setContentView(root)
        buildDice()
    }

    private fun buildDice() {
        diceRow.removeAllViews()
        diceViews = Array(count) {
            TextView(this).apply {
                text = "⚀"
                textSize = 56f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setBackgroundResource(R.drawable.bg_card)
            }
        }
        for (v in diceViews) diceRow.addView(v)
        sumText.text = "Сумма: —"
    }

    private fun roll() {
        if (rolling) return
        rolling = true
        rollButton.isEnabled = false
        var steps = 0
        val animate = object : Runnable {
            override fun run() {
                var sum = 0
                for (v in diceViews) {
                    val value = Random.nextInt(6)
                    v.text = faces[value]
                    sum += value + 1
                }
                sumText.text = "Сумма: $sum"
                steps++
                if (steps < 12) handler.postDelayed(this, 70)
                else {
                    rolling = false
                    rollButton.isEnabled = true
                    vibrate(this@DiceActivity, 50)
                }
            }
        }
        handler.post(animate)
    }
}
