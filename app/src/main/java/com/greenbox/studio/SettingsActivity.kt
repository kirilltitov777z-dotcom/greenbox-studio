package com.greenbox.studio

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private fun section(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.parseColor("#2E7D32"))
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.parseColor("#6B7F6B"))
        setPadding(0, dp(8), 0, dp(2))
    }

    private fun spinner(items: Array<String>, selected: Int, onPick: (Int) -> Unit) =
        Spinner(this).apply {
            val a = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, items)
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = a
            setSelection(selected)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    onPick(pos)
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) { }
            }
        }

    private fun seekBox(max: Int, initial: Int, label: (Int) -> String, onChange: (Int) -> Unit): LinearLayout {
        val valueText = TextView(this).apply {
            text = label(initial)
            textSize = 13f
            setTextColor(Color.parseColor("#43A047"))
        }
        val bar = SeekBar(this).apply {
            this.max = max
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        valueText.text = label(p)
                        onChange(p)
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) { }
                override fun onStopTrackingTouch(s: SeekBar?) { }
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(bar, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(valueText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Настройки")

        val p = dp(18)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p, p, p, p)
        }

        root.addView(section("🎨 Внешний вид"))
        root.addView(label("Тема приложения"))
        root.addView(spinner(arrayOf("Как в системе", "Светлая", "Тёмная"), Prefs.theme) { pos ->
            Prefs.theme = pos
            (application as GreenBoxApp).applyTheme()
            recreate()
        })

        root.addView(section("🖼 Фоторедактор"))
        root.addView(label("Качество сохранения JPEG, %"))
        root.addView(seekBox(50, Prefs.imageQuality - 50, { "${it + 50}%" }) {
            Prefs.imageQuality = it + 50
        })
        root.addView(label("Максимальный размер фото при открытии"))
        root.addView(spinner(arrayOf("1200 px", "1600 px", "2400 px"),
            when (Prefs.maxImageSize) { 1200 -> 0; 2400 -> 2; else -> 1 }) { pos ->
            Prefs.maxImageSize = when (pos) { 0 -> 1200; 2 -> 2400; else -> 1600 }
        })
        root.addView(label("Толщина кисти, px"))
        root.addView(seekBox(38, Prefs.brushSize - 2, { "${it + 2} px" }) {
            Prefs.brushSize = it + 2
        })
        root.addView(label("Цвет кисти по умолчанию"))
        val colorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.BLACK, Color.WHITE)
        for (c in colors) {
            colorRow.addView(Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(40)).apply { setMargins(0, 0, dp(4), 0) }
                setBackgroundColor(c)
                setOnClickListener { Prefs.brushColor = c; toast("Цвет кисти изменён") }
            })
        }
        root.addView(colorRow)
        root.addView(Switch(this).apply {
            text = "Добавлять водяной знак GreenBox при сохранении"
            isChecked = Prefs.watermark
            setOnCheckedChangeListener { _, checked -> Prefs.watermark = checked }
            setPadding(0, dp(10), 0, dp(2))
        })

        root.addView(section("🎮 Игры"))
        root.addView(Switch(this).apply {
            text = "Показывать сетку в змейке и 2048"
            isChecked = Prefs.showGrid
            setOnCheckedChangeListener { _, checked -> Prefs.showGrid = checked }
            setPadding(0, dp(10), 0, dp(2))
        })
        root.addView(Switch(this).apply {
            text = "Вибрация при событиях в играх"
            isChecked = Prefs.vibration
            setOnCheckedChangeListener { _, checked -> Prefs.vibration = checked }
            setPadding(0, dp(10), 0, dp(2))
        })

        root.addView(section("ℹ️ О приложении"))
        root.addView(TextView(this).apply {
            text = "GreenBox 2.0 · фоторедактор + 6 мини-игр\nСделано на Kotlin 🌿"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7F6B"))
        })
        root.addView(Button(this).apply {
            text = "Сбросить настройки"
            setBackgroundResource(R.drawable.btn_soft)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            setOnClickListener {
                Prefs.reset()
                toast("Настройки сброшены")
                recreate()
            }
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
