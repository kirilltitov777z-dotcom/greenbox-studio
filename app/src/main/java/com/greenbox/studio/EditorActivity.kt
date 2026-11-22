package com.greenbox.studio

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class EditorActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var drawView: DrawView
    private lateinit var adjustPanel: ViewGroup
    private lateinit var drawPanel: ViewGroup
    private lateinit var adjustTitle: TextView

    private var bitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private var adjustBase: Bitmap? = null
    private var preview: Bitmap? = null
    private val history = ArrayList<Bitmap>()
    private var histPos = -1
    private var adjustMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        imageView = findViewById(R.id.imageView)
        drawView = findViewById(R.id.drawView)
        adjustPanel = findViewById(R.id.adjustPanel)
        drawPanel = findViewById(R.id.drawPanel)
        adjustTitle = findViewById(R.id.adjustTitle)

        setBitmap(ImageUtils.placeholder(900, 900))

        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(i, "Выберите фото"), REQ_OPEN)
        }
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (i.resolveActivity(packageManager) != null) startActivityForResult(i, REQ_CAMERA)
            else toast("Камера недоступна")
        }
        findViewById<Button>(R.id.btnUndo).setOnClickListener { undo() }
        findViewById<Button>(R.id.btnRedo).setOnClickListener { redo() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveImage() }
        findViewById<Button>(R.id.btnShare).setOnClickListener { shareImage() }
        findViewById<Button>(R.id.btnRandom).setOnClickListener {
            bitmap?.let { setBitmap(ImageUtils.filter(it, (1..14).random())) } ?: toast("Сначала откройте фото")
        }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            originalBitmap?.let { setBitmap(it.copy(Bitmap.Config.ARGB_8888, true)) }
                ?: toast("Сначала откройте фото")
        }

        bindRow(R.id.filterRow, true)
        bindRow(R.id.toolRow, false)

        findViewById<SeekBar>(R.id.adjustSeek).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    preview = makePreview(progress - 100)
                    preview?.let { imageView.setImageBitmap(it) }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            })
        findViewById<Button>(R.id.adjustOk).setOnClickListener {
            preview?.let { setBitmap(it) }
            adjustPanel.visibility = View.GONE
        }
        findViewById<Button>(R.id.adjustCancel).setOnClickListener {
            adjustBase?.let { imageView.setImageBitmap(it) }
            adjustPanel.visibility = View.GONE
        }

        val colors = intArrayOf(Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        for (i in colors.indices) {
            val c = colors[i]
            drawPanel.getChildAt(i + 1)?.setOnClickListener {
                drawView.setColor(c)
                Prefs.brushColor = c
            }
        }
        findViewById<Button>(R.id.drawDone).setOnClickListener {
            drawView.merge()?.let { setBitmap(it) }
            stopDrawing()
        }
        findViewById<Button>(R.id.drawClear).setOnClickListener { drawView.clear() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        try {
            when (requestCode) {
                REQ_OPEN -> {
                    val uri = data.data ?: return
                    val bytes = ImageUtils.readBytes(contentResolver.openInputStream(uri))
                    val b = ImageUtils.decodeSampled(bytes, Prefs.maxImageSize)
                    if (b != null) {
                        originalBitmap = b.copy(Bitmap.Config.ARGB_8888, true)
                        setBitmap(b)
                    } else toast("Не удалось открыть фото")
                }
                REQ_CAMERA -> {
                    val b = data.extras?.get("data") as? Bitmap
                    if (b != null) {
                        val copy = b.copy(Bitmap.Config.ARGB_8888, true)
                        originalBitmap = copy.copy(Bitmap.Config.ARGB_8888, true)
                        setBitmap(copy)
                    }
                }
            }
        } catch (e: Exception) {
            toast("Ошибка: ${e.message}")
        }
    }

    private fun bindRow(rowId: Int, filters: Boolean) {
        val row = findViewById<ViewGroup>(rowId)
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i) ?: continue
            val tag = child.tag ?: continue
            val index = tag.toString().toIntOrNull() ?: continue
            child.setOnClickListener {
                val b = bitmap
                if (b == null) {
                    toast("Сначала откройте фото")
                    return@setOnClickListener
                }
                if (filters) setBitmap(ImageUtils.filter(b, index)) else onTool(index)
            }
        }
    }

    private fun onTool(index: Int) {
        when (index) {
            0 -> openAdjust(0)
            1 -> openAdjust(1)
            2 -> openAdjust(2)
            3 -> bitmap?.let { setBitmap(ImageUtils.rotate(it, 90f, false, false)) }
            4 -> bitmap?.let { setBitmap(ImageUtils.rotate(it, 0f, true, false)) }
            5 -> bitmap?.let { setBitmap(ImageUtils.rotate(it, 0f, false, true)) }
            6 -> bitmap?.let { setBitmap(ImageUtils.cropCenter(it, 1f)) }
            7 -> bitmap?.let { setBitmap(ImageUtils.cropCenter(it, 4f / 3f)) }
            8 -> bitmap?.let { setBitmap(ImageUtils.cropCenter(it, 16f / 9f)) }
            9 -> askText()
            10 -> askSticker()
            11 -> startDrawing()
        }
    }

    private fun openAdjust(mode: Int) {
        val b = bitmap ?: return
        adjustMode = mode
        adjustBase = b.copy(Bitmap.Config.ARGB_8888, true)
        preview = adjustBase
        adjustTitle.text = when (mode) {
            0 -> "Яркость"
            1 -> "Контраст"
            else -> "Насыщенность"
        }
        findViewById<SeekBar>(R.id.adjustSeek).progress = 100
        adjustPanel.visibility = View.VISIBLE
    }

    private fun makePreview(value: Int): Bitmap? {
        val base = adjustBase ?: return null
        val b: Int
        val c: Int
        val s: Int
        when (adjustMode) {
            0 -> { b = value; c = 0; s = 0 }
            1 -> { b = 0; c = value; s = 0 }
            else -> { b = 0; c = 0; s = value }
        }
        return ImageUtils.adjust(base, b, c, s)
    }

    private fun startDrawing() {
        val b = bitmap ?: return
        drawView.setBitmap(b)
        drawView.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        drawPanel.visibility = View.VISIBLE
    }

    private fun stopDrawing() {
        drawView.visibility = View.GONE
        drawPanel.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        bitmap?.let { imageView.setImageBitmap(it) }
    }

    private fun askText() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_text, null)
        val input = view.findViewById<EditText>(R.id.textInput)
        val spinner = view.findViewById<Spinner>(R.id.colorSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.text_colors))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        AlertDialog.Builder(this)
            .setTitle("Добавить текст")
            .setView(view)
            .setPositiveButton("Добавить") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    toast("Введите текст")
                    return@setPositiveButton
                }
                val palette = intArrayOf(Color.WHITE, Color.BLACK, Color.RED, Color.GREEN,
                    Color.BLUE, Color.YELLOW, 0xFFE91E63.toInt())
                addText(text, palette[spinner.selectedItemPosition.coerceIn(0, palette.lastIndex)])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addText(text: String, color: Int) {
        val b = bitmap ?: return
        val out = b.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            textSize = maxOf(28f, out.width / 12f)
        }
        while (p.measureText(text) > out.width * 0.9f && p.textSize > 12f) p.textSize *= 0.9f
        val outline = Paint(p).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(2f, p.textSize / 14f)
            this.color = if (color == Color.WHITE) Color.BLACK else Color.WHITE
        }
        val x = out.width / 2f
        val y = out.height / 2f
        c.drawText(text, x, y, outline)
        c.drawText(text, x, y, p)
        setBitmap(out)
    }

    private fun askSticker() {
        val emojis = arrayOf("❤️", "😀", "🔥", "⭐", "🌈", "🐱", "🦄", "🎉",
            "✨", "🍕", "⚽", "🎵", "🌻", "💎", "🍀", "😎")
        AlertDialog.Builder(this)
            .setTitle("Выберите стикер")
            .setItems(emojis) { _, which -> addSticker(emojis[which]) }
            .show()
    }

    private fun addSticker(emoji: String) {
        val b = bitmap ?: return
        val out = b.copy(Bitmap.Config.ARGB_8888, true)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = out.width / 5f
        }
        val x = out.width * (0.25f + Math.random().toFloat() * 0.5f)
        val y = out.height * (0.25f + Math.random().toFloat() * 0.5f)
        Canvas(out).drawText(emoji, x, y, p)
        setBitmap(out)
    }

    private fun setBitmap(b: Bitmap) {
        bitmap = b
        imageView.setImageBitmap(b)
        pushHistory(b)
    }

    private fun pushHistory(b: Bitmap) {
        val copy = b.copy(Bitmap.Config.ARGB_8888, true)
        while (histPos < history.size - 1 && history.isNotEmpty()) history.removeAt(history.lastIndex)
        history.add(copy)
        histPos = history.lastIndex
        if (history.size > MAX_HISTORY) {
            history.removeAt(0)
            histPos--
        }
    }

    private fun showHistory(pos: Int) {
        histPos = pos
        bitmap = history[pos].copy(Bitmap.Config.ARGB_8888, true)
        imageView.setImageBitmap(bitmap)
    }

    private fun undo() {
        if (histPos > 0) showHistory(histPos - 1) else toast("Больше нечего отменять")
    }

    private fun redo() {
        if (histPos < history.size - 1) showHistory(histPos + 1) else toast("Больше нечего повторить")
    }

    private fun addWatermark(src: Bitmap): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            textAlign = Paint.Align.RIGHT
            textSize = maxOf(20f, src.width / 28f)
        }
        Canvas(out).drawText("GreenBox 🌿", src.width - p.textSize, src.height - p.textSize / 2, p)
        return out
    }

    private fun saveImage(): Uri? {
        val b = bitmap ?: return null
        val out = if (Prefs.watermark) addWatermark(b) else b
        val name = "GreenBox_${System.currentTimeMillis()}.jpg"
        try {
            val saved = MediaStore.Images.Media.insertImage(contentResolver, out, name, "GreenBox")
            if (saved != null) {
                toast("Сохранено в галерею")
                return Uri.parse(saved)
            }
        } catch (t: Throwable) {
            // Fall back to app storage below.
        }
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "GreenBox")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, name)
            FileOutputStream(file).use {
                out.compress(Bitmap.CompressFormat.JPEG, Prefs.imageQuality, it)
            }
            toast("Сохранено: ${file.absolutePath}")
            return Uri.fromFile(file)
        } catch (e: Exception) {
            toast("Не удалось сохранить: ${e.message}")
            return null
        }
    }

    private fun shareImage() {
        val uri = saveImage() ?: return
        if (uri.scheme != "content") {
            toast("Поделиться можно после сохранения в галерею")
            return
        }
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivity(Intent.createChooser(i, "Поделиться фото"))
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val REQ_OPEN = 11
        const val REQ_CAMERA = 12
        const val MAX_HISTORY = 20
    }
}
