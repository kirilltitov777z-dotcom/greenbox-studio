package com.greenbox.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** Drawing layer that displays the bitmap and stores strokes in bitmap coordinates. */
class DrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private val paths = ArrayList<Path>()
    private val paints = ArrayList<Paint>()
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null
    private var color = -65536
    private var stroke = 12f
    private var scale = 1f
    private var left = 0f
    private var top = 0f

    init {
        setWillNotDraw(false)
    }

    fun setBitmap(b: Bitmap?) {
        bitmap = b
        paths.clear()
        paints.clear()
        stroke = (b?.width ?: 800) / 130f + Prefs.brushSize
        color = Prefs.brushColor
        invalidate()
    }

    fun setColor(c: Int) {
        color = c
        currentPaint?.color = c
    }

    fun clear() {
        paths.clear()
        paints.clear()
        invalidate()
    }

    private fun computeLayout() {
        val b = bitmap
        if (b == null) {
            scale = 1f
            left = 0f
            top = 0f
            return
        }
        scale = minOf(width.toFloat() / b.width, height.toFloat() / b.height)
        left = (width - b.width * scale) / 2f
        top = (height - b.height * scale) / 2f
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        computeLayout()
        bitmap?.let {
            val dst = RectF(left, top, left + it.width * scale, top + it.height * scale)
            c.drawBitmap(it, null, dst, null)
        }
        c.save()
        c.translate(left, top)
        c.scale(scale, scale)
        for (i in paths.indices) c.drawPath(paths[i], paints[i])
        val cp = currentPath
        val cpa = currentPaint
        if (cp != null && cpa != null) c.drawPath(cp, cpa)
        c.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val b = bitmap ?: return false
        computeLayout()
        val x = (e.x - left) / scale
        val y = (e.y - top) / scale
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val path = Path().apply { moveTo(x, y) }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = this@DrawView.color
                    style = Paint.Style.STROKE
                    strokeWidth = this@DrawView.stroke
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                currentPath = path
                currentPaint = paint
                paths.add(path)
                paints.add(paint)
            }
            MotionEvent.ACTION_MOVE -> currentPath?.lineTo(x, y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath = null
                currentPaint = null
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun merge(): Bitmap? {
        val b = bitmap ?: return null
        val out = b.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        for (i in paths.indices) c.drawPath(paths[i], paints[i])
        return out
    }
}
