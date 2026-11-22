package com.greenbox.studio

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MemoryActivity : AppCompatActivity() {

    private val emojis = arrayOf("🍎", "🍌", "🍇", "🍓", "🐱", "🐶", "🚗", "⭐")
    private val deck = ArrayList<String>()
    private lateinit var cards: Array<Button>
    private lateinit var info: TextView
    private val revealed = BooleanArray(16)
    private val matched = BooleanArray(16)
    private var first = -1
    private var second = -1
    private var moves = 0
    private var pairs = 0
    private var busy = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Память")

        val p = dp(12)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        info = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#2E7D32"))
        }
        root.addView(info)

        cards = Array(16) { Button(this) }
        for (row in 0..3) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0..3) {
                val pos = row * 4 + col
                cards[pos].apply {
                    text = "❓"
                    textSize = 26f
                    setBackgroundResource(R.drawable.btn_soft)
                    layoutParams = LinearLayout.LayoutParams(0, dp(70), 1f).apply {
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                    setOnClickListener { flip(pos) }
                }
                line.addView(cards[pos])
            }
            root.addView(line)
        }

        root.addView(Button(this).apply {
            text = "Заново"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = p }
            setOnClickListener { newGame() }
        })

        setContentView(root)
        newGame()
    }

    private fun newGame() {
        deck.clear()
        for (e in emojis) {
            deck.add(e)
            deck.add(e)
        }
        deck.shuffle()
        for (i in 0..15) {
            revealed[i] = false
            matched[i] = false
            cards[i].text = "❓"
        }
        first = -1
        second = -1
        moves = 0
        pairs = 0
        busy = false
        updateInfo()
    }

    private fun updateInfo() {
        info.text = "Ходы: $moves · Пары: $pairs/8"
    }

    private fun flip(pos: Int) {
        if (busy || matched[pos] || revealed[pos]) return
        revealed[pos] = true
        cards[pos].text = deck[pos]
        if (first == -1) {
            first = pos
            return
        }
        second = pos
        moves++
        updateInfo()
        busy = true
        val a = first
        val b = second
        handler.postDelayed(Runnable {
            if (deck[a] == deck[b]) {
                matched[a] = true
                matched[b] = true
                pairs++
                updateInfo()
                vibrate(this, 40)
                first = -1
                second = -1
                busy = false
                if (pairs == 8) Toast.makeText(this, "Все пары найдены! 🎉", Toast.LENGTH_SHORT).show()
            } else {
                handler.postDelayed(Runnable {
                    cards[a].text = "❓"
                    cards[b].text = "❓"
                    revealed[a] = false
                    revealed[b] = false
                    first = -1
                    second = -1
                    busy = false
                }, 450)
            }
        }, 650)
    }
}
