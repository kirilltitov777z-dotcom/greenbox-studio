package com.greenbox.studio

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GamesActivity : AppCompatActivity() {

    private val names = arrayOf(
        "🐍  Змейка", "🔢  2048", "❌⭕  Крестики-нолики",
        "🧠  Память", "🎲  Кубики", "⚡  Реакция"
    )
    private val descriptions = arrayOf(
        "Ешьте яблоки и не врезайтесь в стену",
        "Сливайте плитки и соберите 2048",
        "Сразитесь с компьютером",
        "Найдите 8 пар карточек",
        "Бросайте от 1 до 6 кубиков",
        "Нажмите как можно быстрее"
    )
    private val targets = arrayOf(
        SnakeActivity::class.java, Game2048Activity::class.java, TicTacToeActivity::class.java,
        MemoryActivity::class.java, DiceActivity::class.java, ReactionActivity::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Игры")

        val p = dp(18)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p, p, p, p)
        }

        root.addView(TextView(this).apply {
            text = "🎮 Mini-игры"
            textSize = 28f
            setTextColor(Color.parseColor("#2E7D32"))
        })
        root.addView(TextView(this).apply {
            text = "Выберите игру — всё работает офлайн"
            textSize = 14f
            setTextColor(Color.parseColor("#6B7F6B"))
            setPadding(0, dp(4), 0, dp(14))
        })

        root.addView(Button(this).apply {
            text = "🎲 Случайная игра"
            setBackgroundResource(R.drawable.btn_green)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setOnClickListener { startActivity(Intent(this@GamesActivity, targets[Random.nextInt(targets.size)])) }
        })

        for (i in names.indices) {
            val target = targets[i]
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
                setOnClickListener { startActivity(Intent(this@GamesActivity, target)) }
            }
            card.addView(TextView(this).apply {
                text = names[i]
                textSize = 20f
                setTextColor(Color.parseColor("#1B2B1B"))
            })
            card.addView(TextView(this).apply {
                text = descriptions[i]
                textSize = 13f
                setTextColor(Color.parseColor("#6B7F6B"))
                setPadding(0, dp(4), 0, 0)
            })
            card.addView(TextView(this).apply {
                text = "Играть →"
                textSize = 13f
                setTextColor(Color.parseColor("#43A047"))
                gravity = Gravity.END
                setPadding(0, dp(8), 0, 0)
            })
            root.addView(card)
        }

        scroll.addView(root)
        setContentView(scroll)
    }
}
