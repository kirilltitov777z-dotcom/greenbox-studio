package com.greenbox.studio

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

interface SnakeListener {
    fun onScore(score: Int)
    fun onGameOver(score: Int)
}

class SnakeActivity : AppCompatActivity(), SnakeListener {

    private lateinit var scoreText: TextView
    private lateinit var game: SnakeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Змейка")

        val p = dp(12)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        scoreText = TextView(this).apply {
            text = "Очки: 0"
            textSize = 18f
            setTextColor(Color.parseColor("#2E7D32"))
        }
        root.addView(scoreText)

        game = SnakeView(this).apply {
            listener = this@SnakeActivity
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = p; bottomMargin = p }
        }
        root.addView(game)

        val pad = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val dirs = arrayOf(intArrayOf(0, -1), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1))
        val labels = arrayOf("↑", "←", "→", "↓")
        for (i in dirs.indices) {
            val dx = dirs[i][0]
            val dy = dirs[i][1]
            pad.addView(Button(this).apply {
                text = labels[i]
                textSize = 18f
                setBackgroundResource(R.drawable.btn_soft)
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(48)).apply { leftMargin = dp(4) }
                setOnClickListener { game.setDirection(dx, dy) }
            })
        }
        root.addView(pad)

        root.addView(Button(this).apply {
            text = "Новая игра"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = p }
            setOnClickListener { game.reset() }
        })

        setContentView(root)
        game.reset()
    }

    override fun onResume() { super.onResume(); game.resume() }
    override fun onPause() { super.onPause(); game.pause() }

    override fun onScore(score: Int) { scoreText.text = "Очки: $score" }
    override fun onGameOver(score: Int) {
        scoreText.text = "Игра окончена · очки: $score"
        Toast.makeText(this, "Игра окончена!", Toast.LENGTH_SHORT).show()
    }

    class SnakeView(context: android.content.Context) : View(context), OnTouchListener {

        var listener: SnakeListener? = null
        private val snake = ArrayList<Point>()
        private var food: Point? = null
        private var dirX = 1
        private var dirY = 0
        private var score = 0
        private var running = false
        private var over = false
        private var cell = 20f
        private var ox = 0f
        private var oy = 0f
        private var downX = 0f
        private var downY = 0f
        private val handler = Handler(Looper.getMainLooper())
        private val tick = Runnable { stepAndSchedule() }

        init {
            setOnTouchListener(this)
            setBackgroundColor(Color.parseColor("#102A15"))
        }

        private fun stepAndSchedule() {
            if (!running) return
            step()
            if (running) handler.postDelayed(tick, 120)
        }

        fun reset() {
            snake.clear()
            val cy = N / 2
            for (i in 3 downTo 0) snake.add(Point(N / 2 - i, cy))
            dirX = 1
            dirY = 0
            score = 0
            over = false
            listener?.onScore(0)
            spawnFood()
            running = true
            handler.removeCallbacks(tick)
            handler.post(tick)
            invalidate()
        }

        fun resume() {
            if (!over && !running) {
                running = true
                handler.removeCallbacks(tick)
                handler.post(tick)
            }
        }

        fun pause() {
            running = false
            handler.removeCallbacks(tick)
        }

        fun setDirection(dx: Int, dy: Int) {
            if (dirX == -dx && dirY == -dy && snake.size > 1) return
            dirX = dx
            dirY = dy
        }

        private fun spawnFood() {
            repeat(500) {
                val p = Point(Random.nextInt(N), Random.nextInt(N))
                if (!snake.contains(p)) {
                    food = p
                    return
                }
            }
            food = Point(0, 0)
        }

        private fun step() {
            val head = snake[0]
            val next = Point(head.x + dirX, head.y + dirY)
            if (next.x < 0 || next.x >= N || next.y < 0 || next.y >= N || snake.contains(next)) {
                over = true
                running = false
                vibrate(context, 120)
                listener?.onGameOver(score)
                invalidate()
                return
            }
            snake.add(0, next)
            if (food != null && next == food) {
                score++
                vibrate(context, 25)
                listener?.onScore(score)
                spawnFood()
            } else {
                snake.removeAt(snake.lastIndex)
            }
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            cell = minOf(w, h) / N.toFloat()
            ox = (w - cell * N) / 2f
            oy = (h - cell * N) / 2f
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            if (Prefs.showGrid) {
                val grid = Paint().apply {
                    color = Color.parseColor("#1C4224")
                    strokeWidth = 1f
                }
                for (i in 0..N) {
                    c.drawLine(ox + i * cell, oy, ox + i * cell, oy + N * cell, grid)
                    c.drawLine(ox, oy + i * cell, ox + N * cell, oy + i * cell, grid)
                }
            }
            food?.let {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5252") }
                c.drawCircle(ox + it.x * cell + cell / 2, oy + it.y * cell + cell / 2, cell * 0.36f, p)
            }
            for (i in snake.indices.reversed()) {
                val pt = snake[i]
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (i == 0) Color.parseColor("#AEDD6A") else Color.parseColor("#4CAF50")
                }
                val r = RectF(ox + pt.x * cell + 1, oy + pt.y * cell + 1,
                    ox + (pt.x + 1) * cell - 1, oy + (pt.y + 1) * cell - 1)
                c.drawRoundRect(r, cell * 0.25f, cell * 0.25f, p)
            }
            if (over) {
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(),
                    Paint().apply { color = Color.parseColor("#AA000000") })
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = maxOf(24f, cell * 2)
                }
                c.drawText("Игра окончена", width / 2f, height / 2f, p)
                p.textSize = maxOf(16f, cell)
                c.drawText("Очки: $score · «Новая игра»", width / 2f, height / 2f + cell * 2, p)
            }
        }

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y }
                MotionEvent.ACTION_UP -> {
                    val dx = e.x - downX
                    val dy = e.y - downY
                    if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) < 20) return true
                    if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) setDirection(if (dx > 0) 1 else -1, 0)
                    else setDirection(0, if (dy > 0) 1 else -1)
                }
            }
            return true
        }

        companion object { const val N = 20 }
    }
}
