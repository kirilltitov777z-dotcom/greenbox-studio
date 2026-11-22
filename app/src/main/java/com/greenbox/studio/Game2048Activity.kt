package com.greenbox.studio

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.random.Random

interface BoardListener {
    fun onScore(score: Int)
    fun onLose(score: Int)
}

class Game2048Activity : AppCompatActivity(), BoardListener {

    private lateinit var scoreText: TextView
    private lateinit var board: BoardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("2048")

        val p = dp(14)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        scoreText = TextView(this).apply {
            text = "Очки: 0"
            textSize = 20f
            setTextColor(Color.parseColor("#2E7D32"))
        }
        root.addView(scoreText)

        board = BoardView(this).apply {
            listener = this@Game2048Activity
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = p; bottomMargin = p }
        }
        root.addView(board)

        root.addView(Button(this).apply {
            text = "Новая игра"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { board.reset() }
        })
        root.addView(TextView(this).apply {
            text = "Свайпайте влево / вправо / вверх / вниз"
            textSize = 12f
            setTextColor(Color.parseColor("#6B7F6B"))
            setPadding(0, p, 0, 0)
        })

        setContentView(root)
        board.reset()
    }

    override fun onScore(score: Int) { scoreText.text = "Очки: $score" }
    override fun onLose(score: Int) {
        vibrate(this, 150)
        Toast.makeText(this, "Ходов больше нет! Очки: $score", Toast.LENGTH_SHORT).show()
    }

    class BoardView(context: android.content.Context) : View(context), OnTouchListener {

        var listener: BoardListener? = null
        private val cells = Array(4) { IntArray(4) }
        private var score = 0
        private var over = false
        private var downX = 0f
        private var downY = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var cellSize = 100f
        private var gap = 6f
        private var pad = 6f

        init {
            setOnTouchListener(this)
            setBackgroundColor(Color.parseColor("#BBADA0"))
        }

        fun reset() {
            for (row in cells) row.fill(0)
            score = 0
            over = false
            addRandom()
            addRandom()
            listener?.onScore(0)
            invalidate()
        }

        private fun addRandom() {
            val empty = ArrayList<Pair<Int, Int>>()
            for (r in 0..3) for (c in 0..3) if (cells[r][c] == 0) empty.add(r to c)
            if (empty.isEmpty()) return
            val (r, c) = empty[Random.nextInt(empty.size)]
            cells[r][c] = if (Random.nextDouble() < 0.9) 2 else 4
        }

        private fun mergeLine(line: IntArray): IntArray {
            val tmp = IntArray(4)
            var k = 0
            for (v in line) if (v != 0) tmp[k++] = v
            val out = IntArray(4)
            var j = 0
            var i = 0
            while (i < k) {
                if (i + 1 < k && tmp[i] == tmp[i + 1]) {
                    out[j++] = tmp[i] * 2
                    score += tmp[i] * 2
                    i += 2
                } else {
                    out[j++] = tmp[i]
                    i++
                }
            }
            return out
        }

        private fun move(dr: Int, dc: Int) {
            if (over) return
            var changed = false
            if (dc != 0) {
                for (r in 0..3) {
                    val line = IntArray(4) { c -> cells[r][if (dc < 0) c else 3 - c] }
                    val merged = mergeLine(line)
                    for (c in 0..3) cells[r][if (dc < 0) c else 3 - c] = merged[c]
                    if (!line.contentEquals(merged)) changed = true
                }
            } else {
                for (c in 0..3) {
                    val line = IntArray(4) { r -> cells[if (dr < 0) r else 3 - r][c] }
                    val merged = mergeLine(line)
                    for (r in 0..3) cells[if (dr < 0) r else 3 - r][c] = merged[r]
                    if (!line.contentEquals(merged)) changed = true
                }
            }
            if (!changed) return
            addRandom()
            listener?.onScore(score)
            if (isStuck()) {
                over = true
                listener?.onLose(score)
            }
            invalidate()
        }

        private fun isStuck(): Boolean {
            for (r in 0..3) for (c in 0..3) {
                if (cells[r][c] == 0) return false
                if (c < 3 && cells[r][c] == cells[r][c + 1]) return false
                if (r < 3 && cells[r][c] == cells[r + 1][c]) return false
            }
            return true
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            gap = dp(6).toFloat()
            pad = dp(6).toFloat()
            cellSize = (minOf(w, h) - pad * 2 - gap * 3) / 4f
        }

        private fun colorFor(v: Int) = when (v) {
            2 -> 0xFFEEE4DA.toInt()
            4 -> 0xFFEDE0C8.toInt()
            8 -> 0xFFF2B179.toInt()
            16 -> 0xFFF59563.toInt()
            32 -> 0xFFF67C5F.toInt()
            64 -> 0xFFF65E3B.toInt()
            128 -> 0xFFEDCF72.toInt()
            256 -> 0xFFEDCC61.toInt()
            512 -> 0xFFEDC850.toInt()
            1024 -> 0xFFEDC53F.toInt()
            else -> 0xFF3C3A32.toInt()
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            for (r in 0..3) for (col in 0..3) {
                val x = pad + col * (cellSize + gap)
                val y = pad + r * (cellSize + gap)
                val v = cells[r][col]
                paint.color = if (v == 0) 0x66FFFFFF.toInt() else colorFor(v)
                c.drawRoundRect(RectF(x, y, x + cellSize, y + cellSize),
                    dp(6).toFloat(), dp(6).toFloat(), paint)
                if (v != 0) {
                    paint.color = if (v >= 8) Color.WHITE else Color.parseColor("#776E65")
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = cellSize * if (v >= 1024) 0.28f else 0.42f
                    c.drawText(v.toString(), x + cellSize / 2,
                        y + cellSize / 2 + paint.textSize * 0.35f, paint)
                }
            }
            if (over) {
                paint.color = Color.parseColor("#99000000")
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = maxOf(26f, cellSize * 0.7f)
                c.drawText("Конец игры", width / 2f, height / 2f, paint)
            }
        }

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y }
                MotionEvent.ACTION_UP -> {
                    val dx = e.x - downX
                    val dy = e.y - downY
                    if (maxOf(abs(dx), abs(dy)) < 20) return true
                    if (abs(dx) > abs(dy)) move(0, if (dx > 0) 1 else -1)
                    else move(if (dy > 0) 1 else -1, 0)
                }
            }
            return true
        }
    }
}
