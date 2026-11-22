package com.greenbox.studio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Transparent drawing layer that shows the bitmap and stores strokes in bitmap coordinates. */
public class DrawView extends View {

    private Bitmap bitmap;
    private final List<Path> paths = new ArrayList<>();
    private final List<Paint> paints = new ArrayList<>();
    private Path currentPath;
    private Paint currentPaint;
    private int color = Color.RED;
    private float stroke = 12f;
    private float scale = 1f, left = 0f, top = 0f;

    public DrawView(Context c, AttributeSet a) {
        super(c, a);
        setWillNotDraw(false);
    }

    public void setBitmap(Bitmap b) {
        bitmap = b;
        paths.clear();
        paints.clear();
        stroke = Math.max(6f, (b != null ? b.getWidth() : 800) / 130f);
        invalidate();
    }

    public void setColor(int c) {
        color = c;
        if (currentPaint != null) currentPaint.setColor(color);
    }

    public void clear() {
        paths.clear();
        paints.clear();
        invalidate();
    }

    private void computeLayout() {
        if (bitmap == null) {
            scale = 1f; left = 0; top = 0;
            return;
        }
        scale = Math.min((float) getWidth() / bitmap.getWidth(),
                (float) getHeight() / bitmap.getHeight());
        left = (getWidth() - bitmap.getWidth() * scale) / 2f;
        top = (getHeight() - bitmap.getHeight() * scale) / 2f;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        computeLayout();
        if (bitmap != null) {
            RectF dst = new RectF(left, top,
                    left + bitmap.getWidth() * scale, top + bitmap.getHeight() * scale);
            c.drawBitmap(bitmap, null, dst, null);
        }
        c.save();
        c.translate(left, top);
        c.scale(scale, scale);
        for (int i = 0; i < paths.size(); i++) c.drawPath(paths.get(i), paints.get(i));
        if (currentPath != null && currentPaint != null) c.drawPath(currentPath, currentPaint);
        c.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (bitmap == null) return false;
        computeLayout();
        float x = (e.getX() - left) / scale;
        float y = (e.getY() - top) / scale;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                currentPaint.setColor(color);
                currentPaint.setStyle(Paint.Style.STROKE);
                currentPaint.setStrokeWidth(stroke);
                currentPaint.setStrokeCap(Paint.Cap.ROUND);
                currentPaint.setStrokeJoin(Paint.Join.ROUND);
                paths.add(currentPath);
                paints.add(currentPaint);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) currentPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentPath = null;
                currentPaint = null;
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public Bitmap merge() {
        if (bitmap == null) return null;
        Bitmap out = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas c = new Canvas(out);
        for (int i = 0; i < paths.size(); i++) c.drawPath(paths.get(i), paints.get(i));
        return out;
    }
}
