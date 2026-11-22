package com.greenbox.studio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** All image math for the editor lives here. */
public final class ImageUtils {

    public static final int ORIGINAL = 0, GRAY = 1, SEPIA = 2, INVERT = 3, COOL = 4,
            WARM = 5, VINTAGE = 6, NOIR = 7, NEON = 8, PASTEL = 9, SUNSET = 10,
            AQUA = 11, SHARPEN = 12, BLUR = 13, EMBOSS = 14;

    private ImageUtils() { }

    public static byte[] readBytes(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    public static Bitmap decodeSampled(byte[] data, int maxDim) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        int sample = 1;
        int biggest = Math.max(o.outWidth, o.outHeight);
        while (biggest / sample > maxDim) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o);
    }

    public static Bitmap filter(Bitmap src, int type) {
        if (src == null) return null;
        switch (type) {
            case SHARPEN: return convolve(src, new float[]{0,-1,0,-1,5,-1,0,-1,0}, 1);
            case BLUR:    return convolve(src, new float[]{1,1,1,1,1,1,1,1,1}, 9);
            case EMBOSS:  return convolve(src, new float[]{-2,-1,0,-1,1,1,0,1,2}, 1);
            default: break;
        }

        ColorMatrix cm = new ColorMatrix();
        switch (type) {
            case GRAY:
                cm.setSaturation(0f);
                break;
            case SEPIA:
                cm.set(new float[]{
                        0.393f,0.769f,0.189f,0,0,
                        0.349f,0.686f,0.168f,0,0,
                        0.272f,0.534f,0.131f,0,0,
                        0,0,0,1,0});
                break;
            case INVERT:
                cm.set(new float[]{
                        -1,0,0,0,255, 0,-1,0,0,255, 0,0,-1,0,255, 0,0,0,1,0});
                break;
            case COOL:
                cm.set(new float[]{
                        0.92f,0,0,0,-6, 0,1.02f,0,0,2, 0,0,1.18f,0,12, 0,0,0,1,0});
                break;
            case WARM:
                cm.set(new float[]{
                        1.18f,0,0,0,12, 0,1.02f,0,0,2, 0,0,0.84f,0,-12, 0,0,0,1,0});
                break;
            case VINTAGE: {
                ColorMatrix sepia = new ColorMatrix(new float[]{
                        0.393f,0.769f,0.189f,0,0,
                        0.349f,0.686f,0.168f,0,0,
                        0.272f,0.534f,0.131f,0,0,
                        0,0,0,1,0});
                ColorMatrix tone = new ColorMatrix(new float[]{
                        0.85f,0,0,0,18, 0,0.82f,0,0,14, 0,0,0.72f,0,8, 0,0,0,1,0});
                sepia.postConcat(tone);
                cm = sepia;
                break;
            }
            case NOIR:
                cm.setSaturation(0f);
                cm.postConcat(new ColorMatrix(new float[]{
                        1.35f,0,0,0,-45, 0,1.35f,0,0,-45, 0,0,1.35f,0,-45, 0,0,0,1,0}));
                break;
            case NEON:
                cm.setSaturation(2.1f);
                cm.postConcat(new ColorMatrix(new float[]{
                        1.12f,0,0,0,-12, 0,1.12f,0,0,-12, 0,0,1.12f,0,-12, 0,0,0,1,0}));
                break;
            case PASTEL:
                cm.setSaturation(0.55f);
                cm.postConcat(new ColorMatrix(new float[]{
                        0.92f,0,0,0,28, 0,0.92f,0,0,28, 0,0,0.92f,0,28, 0,0,0,1,0}));
                break;
            case SUNSET:
                cm.set(new float[]{
                        1.22f,0,0,0,22, 0,0.92f,0,0,4, 0,0,0.78f,0,-14, 0,0,0,1,0});
                break;
            case AQUA:
                cm.set(new float[]{
                        0.82f,0,0,0,-4, 0,1.1f,0,0,10, 0,0,1.22f,0,18, 0,0,0,1,0});
                break;
            default:
                return src.copy(Bitmap.Config.ARGB_8888, true);
        }
        return applyMatrix(src, cm);
    }

    /** b, c, s are in -100..100 */
    public static Bitmap adjust(Bitmap src, int b, int c, int s) {
        if (src == null) return null;
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation((s + 100) / 100f);
        float f = (c + 100) / 100f;
        float off = b * 2.55f + 128f * (1f - f);
        cm.postConcat(new ColorMatrix(new float[]{
                f,0,0,0,off, 0,f,0,0,off, 0,0,f,0,off, 0,0,0,1,0}));
        return applyMatrix(src, cm);
    }

    private static Bitmap applyMatrix(Bitmap src, ColorMatrix cm) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    public static Bitmap convolve(Bitmap src, float[] kernel, int divisor) {
        int w = src.getWidth(), h = src.getHeight();
        int[] in = new int[w * h], out = new int[w * h];
        src.getPixels(in, 0, w, 0, 0, w, h);
        int side = (int) Math.sqrt(kernel.length), half = side / 2;
        float div = divisor == 0 ? 1 : divisor;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float r = 0, g = 0, bl = 0;
                for (int ky = -half; ky <= half; ky++) {
                    for (int kx = -half; kx <= half; kx++) {
                        int px = Math.min(w - 1, Math.max(0, x + kx));
                        int py = Math.min(h - 1, Math.max(0, y + ky));
                        int p = in[py * w + px];
                        float k = kernel[(ky + half) * side + (kx + half)];
                        r += ((p >> 16) & 255) * k;
                        g += ((p >> 8) & 255) * k;
                        bl += (p & 255) * k;
                    }
                }
                int rr = clamp(Math.round(r / div));
                int gg = clamp(Math.round(g / div));
                int bb = clamp(Math.round(bl / div));
                out[y * w + x] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(out, 0, w, 0, 0, w, h);
        return result;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    public static Bitmap cropCenter(Bitmap src, float ratio) {
        int w = src.getWidth(), h = src.getHeight();
        int nw, nh;
        if (w > h * ratio) {
            nh = h;
            nw = Math.round(h * ratio);
        } else {
            nw = w;
            nh = Math.round(w / ratio);
        }
        int x = (w - nw) / 2, y = (h - nh) / 2;
        return Bitmap.createBitmap(src, x, y, nw, nh);
    }

    public static Bitmap rotate(Bitmap src, float degrees, boolean flipH, boolean flipV) {
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.postRotate(degrees);
        if (flipH) m.postScale(-1, 1);
        if (flipV) m.postScale(1, -1);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    }

    public static Bitmap placeholder(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(0xFFE8F5E9);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF2E7D32);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(Math.max(28, w / 16f));
        c.drawText("Откройте фото 🌿", w / 2f, h / 2f, p);
        p.setTextSize(Math.max(18, w / 26f));
        p.setColor(0xFF6B7F6B);
        c.drawText("Кнопкой «Открыть» выше", w / 2f, h / 2f + p.getTextSize() * 1.8f, p);
        return bmp;
    }
}
