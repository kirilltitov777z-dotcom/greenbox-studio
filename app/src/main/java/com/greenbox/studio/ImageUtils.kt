package com.greenbox.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** All image math for the editor lives here. */
object ImageUtils {
    const val ORIGINAL = 0
    const val GRAY = 1
    const val SEPIA = 2
    const val INVERT = 3
    const val COOL = 4
    const val WARM = 5
    const val VINTAGE = 6
    const val NOIR = 7
    const val NEON = 8
    const val PASTEL = 9
    const val SUNSET = 10
    const val AQUA = 11
    const val SHARPEN = 12
    const val BLUR = 13
    const val EMBOSS = 14

    fun readBytes(input: InputStream?): ByteArray {
        if (input == null) throw IllegalArgumentException("Пустой поток")
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        input.use {
            var n = it.read(buf)
            while (n != -1) {
                out.write(buf, 0, n)
                n = it.read(buf)
            }
        }
        return out.toByteArray()
    }

    fun decodeSampled(data: ByteArray, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, opts)
        var sample = 1
        while (max(opts.outWidth, opts.outHeight) / sample > maxDim) sample *= 2
        return BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }

    fun filter(src: Bitmap, type: Int): Bitmap {
        when (type) {
            SHARPEN -> return convolve(src, floatArrayOf(0f,-1f,0f, -1f,5f,-1f, 0f,-1f,0f), 1)
            BLUR -> return convolve(src, floatArrayOf(1f,1f,1f, 1f,1f,1f, 1f,1f,1f), 9)
            EMBOSS -> return convolve(src, floatArrayOf(-2f,-1f,0f, -1f,1f,1f, 0f,1f,2f), 1)
        }

        var cm = ColorMatrix()
        when (type) {
            GRAY -> cm.setSaturation(0f)
            SEPIA -> cm.set(floatArrayOf(
                0.393f,0.769f,0.189f,0f,0f,
                0.349f,0.686f,0.168f,0f,0f,
                0.272f,0.534f,0.131f,0f,0f,
                0f,0f,0f,1f,0f))
            INVERT -> cm.set(floatArrayOf(
                -1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f,
                0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f))
            COOL -> cm.set(floatArrayOf(
                0.92f,0f,0f,0f,-6f, 0f,1.02f,0f,0f,2f,
                0f,0f,1.18f,0f,12f, 0f,0f,0f,1f,0f))
            WARM -> cm.set(floatArrayOf(
                1.18f,0f,0f,0f,12f, 0f,1.02f,0f,0f,2f,
                0f,0f,0.84f,0f,-12f, 0f,0f,0f,1f,0f))
            VINTAGE -> {
                val sepia = ColorMatrix(floatArrayOf(
                    0.393f,0.769f,0.189f,0f,0f,
                    0.349f,0.686f,0.168f,0f,0f,
                    0.272f,0.534f,0.131f,0f,0f,
                    0f,0f,0f,1f,0f))
                sepia.postConcat(ColorMatrix(floatArrayOf(
                    0.85f,0f,0f,0f,18f, 0f,0.82f,0f,0f,14f,
                    0f,0f,0.72f,0f,8f, 0f,0f,0f,1f,0f)))
                cm = sepia
            }
            NOIR -> {
                cm.setSaturation(0f)
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.35f,0f,0f,0f,-45f, 0f,1.35f,0f,0f,-45f,
                    0f,0f,1.35f,0f,-45f, 0f,0f,0f,1f,0f)))
            }
            NEON -> {
                cm.setSaturation(2.1f)
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.12f,0f,0f,0f,-12f, 0f,1.12f,0f,0f,-12f,
                    0f,0f,1.12f,0f,-12f, 0f,0f,0f,1f,0f)))
            }
            PASTEL -> {
                cm.setSaturation(0.55f)
                cm.postConcat(ColorMatrix(floatArrayOf(
                    0.92f,0f,0f,0f,28f, 0f,0.92f,0f,0f,28f,
                    0f,0f,0.92f,0f,28f, 0f,0f,0f,1f,0f)))
            }
            SUNSET -> cm.set(floatArrayOf(
                1.22f,0f,0f,0f,22f, 0f,0.92f,0f,0f,4f,
                0f,0f,0.78f,0f,-14f, 0f,0f,0f,1f,0f))
            AQUA -> cm.set(floatArrayOf(
                0.82f,0f,0f,0f,-4f, 0f,1.1f,0f,0f,10f,
                0f,0f,1.22f,0f,18f, 0f,0f,0f,1f,0f))
            else -> return src.copy(Bitmap.Config.ARGB_8888, true)
        }
        return applyMatrix(src, cm)
    }

    /** b, c, s are in -100..100 */
    fun adjust(src: Bitmap, b: Int, c: Int, s: Int): Bitmap {
        val cm = ColorMatrix()
        cm.setSaturation((s + 100) / 100f)
        val f = (c + 100) / 100f
        val off = b * 2.55f + 128f * (1f - f)
        cm.postConcat(ColorMatrix(floatArrayOf(
            f,0f,0f,0f,off, 0f,f,0f,0f,off,
            0f,0f,f,0f,off, 0f,0f,0f,1f,0f)))
        return applyMatrix(src, cm)
    }

    private fun applyMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun convolve(src: Bitmap, kernel: FloatArray, divisor: Int): Bitmap {
        val w = src.width
        val h = src.height
        val input = IntArray(w * h)
        val output = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        val side = sqrt(kernel.size.toDouble()).toInt()
        val half = side / 2
        val div = if (divisor == 0) 1f else divisor.toFloat()
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0f
                var g = 0f
                var bl = 0f
                for (ky in -half..half) {
                    for (kx in -half..half) {
                        val px = min(w - 1, max(0, x + kx))
                        val py = min(h - 1, max(0, y + ky))
                        val p = input[py * w + px]
                        val k = kernel[(ky + half) * side + (kx + half)]
                        r += ((p shr 16) and 255) * k
                        g += ((p shr 8) and 255) * k
                        bl += (p and 255) * k
                    }
                }
                val rr = clamp((r / div).roundToInt())
                val gg = clamp((g / div).roundToInt())
                val bb = clamp((bl / div).roundToInt())
                output[y * w + x] = -0x1000000 or (rr shl 16) or (gg shl 8) or bb
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(output, 0, w, 0, 0, w, h)
        }
    }

    private fun clamp(v: Int) = when {
        v < 0 -> 0
        v > 255 -> 255
        else -> v
    }

    fun cropCenter(src: Bitmap, ratio: Float): Bitmap {
        val w = src.width
        val h = src.height
        val nw: Int
        val nh: Int
        if (w > h * ratio) {
            nh = h
            nw = (h * ratio).roundToInt()
        } else {
            nw = w
            nh = (w / ratio).roundToInt()
        }
        return Bitmap.createBitmap(src, (w - nw) / 2, (h - nh) / 2, nw, nh)
    }

    fun rotate(src: Bitmap, degrees: Float, flipH: Boolean, flipV: Boolean): Bitmap {
        val m = Matrix().apply {
            postRotate(degrees)
            if (flipH) postScale(-1f, 1f)
            if (flipV) postScale(1f, -1f)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun placeholder(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFFE8F5E9.toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2E7D32.toInt()
            textAlign = Paint.Align.CENTER
            textSize = max(28f, w / 16f)
        }
        c.drawText("Откройте фото 🌿", w / 2f, h / 2f, p)
        p.textSize = max(18f, w / 26f)
        p.color = 0xFF6B7F6B.toInt()
        c.drawText("Кнопкой «Открыть» выше", w / 2f, h / 2f + p.textSize * 1.8f, p)
        return bmp
    }
}
