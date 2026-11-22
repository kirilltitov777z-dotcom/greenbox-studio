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
import kotlin.random.Random

class TicTacToeActivity : AppCompatActivity() {

    private val board = CharArray(9) { ' ' }
    private val buttons = Array(9) { Button(this) }
    private lateinit var status: TextView
    private lateinit var scoreLine: TextView
    private var playerTurn = true
    private var finished = false
    private var wins = 0
    private var losses = 0
    private var draws = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Крестики-нолики")

        val p = dp(14)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        status = TextView(this).apply {
            text = "Вы — X. Ваш ход!"
            textSize = 20f
            setTextColor(Color.parseColor("#2E7D32"))
        }
        root.addView(status)
        scoreLine = TextView(this).apply {
            text = "Победы: 0 · Поражения: 0 · Ничьи: 0"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7F6B"))
            setPadding(0, dp(4), 0, dp(14))
        }
        root.addView(scoreLine)

        for (row in 0..2) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0..2) {
                val pos = row * 3 + col
                buttons[pos].apply {
                    text = "·"
                    textSize = 30f
                    setBackgroundResource(R.drawable.btn_soft)
                    layoutParams = LinearLayout.LayoutParams(0, dp(80), 1f).apply {
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                    setOnClickListener { onCell(pos) }
                }
                line.addView(buttons[pos])
            }
            root.addView(line)
        }

        root.addView(Button(this).apply {
            text = "Новая партия"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
            setOnClickListener { newRound() }
        })

        setContentView(root)
        newRound()
    }

    private fun newRound() {
        for (i in 0..8) {
            board[i] = ' '
            buttons[i].text = "·"
        }
        playerTurn = true
        finished = false
        status.text = "Вы — X. Ваш ход!"
    }

    private fun onCell(pos: Int) {
        if (finished || !playerTurn || board[pos] != ' ') return
        board[pos] = 'X'
        buttons[pos].text = "X"
        buttons[pos].setTextColor(Color.parseColor("#2E7D32"))
        if (finishIfNeeded()) return
        playerTurn = false
        status.text = "Компьютер думает..."
        handler.postDelayed(Runnable { aiMove() }, 350)
    }

    private fun aiMove() {
        if (finished) return
        var best = -1000
        val candidates = ArrayList<Int>()
        for (i in 0..8) {
            if (board[i] != ' ') continue
            board[i] = 'O'
            val score = minimax(false)
            board[i] = ' '
            if (score > best) {
                best = score
                candidates.clear()
                candidates.add(i)
            } else if (score == best) candidates.add(i)
        }
        if (candidates.isEmpty()) return
        val move = candidates[Random.nextInt(candidates.size)]
        board[move] = 'O'
        buttons[move].text = "O"
        buttons[move].setTextColor(Color.parseColor("#E65100"))
        vibrate(this, 20)
        if (!finishIfNeeded()) {
            playerTurn = true
            status.text = "Ваш ход!"
        }
    }

    private fun evaluate(): Int {
        val lines = arrayOf(intArrayOf(0,1,2), intArrayOf(3,4,5), intArrayOf(6,7,8),
            intArrayOf(0,3,6), intArrayOf(1,4,7), intArrayOf(2,5,8),
            intArrayOf(0,4,8), intArrayOf(2,4,6))
        for (l in lines) {
            val a = board[l[0]]
            if (a != ' ' && a == board[l[1]] && a == board[l[2]]) {
                return if (a == 'X') -10 else 10
            }
        }
        for (cell in board) if (cell == ' ') return 0
        return 2
    }

    private fun minimax(maximizing: Boolean): Int {
        val res = evaluate()
        if (res == -10 || res == 10) return res
        if (res == 2) return 0
        var best = if (maximizing) -1000 else 1000
        for (i in 0..8) {
            if (board[i] != ' ') continue
            board[i] = if (maximizing) 'O' else 'X'
            val s = minimax(!maximizing)
            board[i] = ' '
            best = if (maximizing) maxOf(best, s) else minOf(best, s)
        }
        return best
    }

    private fun finishIfNeeded(): Boolean {
        val res = evaluate()
        if (res == 0) return false
        finished = true
        when (res) {
            -10 -> {
                wins++
                status.text = "Вы победили! 🎉"
                Toast.makeText(this, "Победа! 🎉", Toast.LENGTH_SHORT).show()
                vibrate(this, 100)
            }
            10 -> {
                losses++
                status.text = "Компьютер победил!"
                Toast.makeText(this, "Вы проиграли 😔", Toast.LENGTH_SHORT).show()
            }
            else -> {
                draws++
                status.text = "Ничья"
            }
        }
        scoreLine.text = "Победы: $wins · Поражения: $losses · Ничьи: $draws"
        return true
    }
}
