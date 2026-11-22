package com.greenbox.studio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class EditorActivity extends Activity {

    private static final int REQ_OPEN = 11, REQ_CAMERA = 12;
    private static final int MAX_HISTORY = 20;

    private ImageView imageView;
    private DrawView drawView;
    private Bitmap bitmap;
    private Bitmap adjustBase, preview;
    private final ArrayList<Bitmap> history = new ArrayList<>();
    private int histPos = -1;
    private int adjustMode = 0;
    private ViewGroup adjustPanel, drawPanel;
    private TextView adjustTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        imageView = findViewById(R.id.imageView);
        drawView = findViewById(R.id.drawView);
        adjustPanel = findViewById(R.id.adjustPanel);
        drawPanel = findViewById(R.id.drawPanel);
        adjustTitle = findViewById(R.id.adjustTitle);

        setBitmap(ImageUtils.placeholder(900, 900));

        findViewById(R.id.btnOpen).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "Выберите фото"), REQ_OPEN);
        });
        findViewById(R.id.btnCamera).setOnClickListener(v -> {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (i.resolveActivity(getPackageManager()) != null)
                startActivityForResult(i, REQ_CAMERA);
            else toast("Камера недоступна");
        });
        findViewById(R.id.btnUndo).setOnClickListener(v -> undo());
        findViewById(R.id.btnRedo).setOnClickListener(v -> redo());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveImage());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareImage());

        bindRow(R.id.filterRow, true);
        bindRow(R.id.toolRow, false);

        SeekBar seek = findViewById(R.id.adjustSeek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                preview = makePreview(progress - 100);
                imageView.setImageBitmap(preview);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        findViewById(R.id.adjustOk).setOnClickListener(v -> {
            if (preview != null) setBitmap(preview);
            adjustPanel.setVisibility(View.GONE);
        });
        findViewById(R.id.adjustCancel).setOnClickListener(v -> {
            imageView.setImageBitmap(adjustBase);
            adjustPanel.setVisibility(View.GONE);
        });

        int[] colors = {Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
        for (int i = 0; i < colors.length; i++) {
            final int c = colors[i];
            View child = drawPanel.getChildAt(i + 1);
            if (child != null) child.setOnClickListener(v -> drawView.setColor(c));
        }
        findViewById(R.id.drawDone).setOnClickListener(v -> {
            Bitmap merged = drawView.merge();
            if (merged != null) setBitmap(merged);
            stopDrawing();
        });
        findViewById(R.id.drawClear).setOnClickListener(v -> drawView.clear());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        try {
            if (req == REQ_OPEN && data.getData() != null) {
                byte[] bytes = ImageUtils.readBytes(getContentResolver().openInputStream(data.getData()));
                Bitmap b = ImageUtils.decodeSampled(bytes, 1600);
                if (b != null) setBitmap(b); else toast("Не удалось открыть фото");
            } else if (req == REQ_CAMERA) {
                Bitmap b = (Bitmap) data.getExtras().get("data");
                if (b != null) setBitmap(b.copy(Bitmap.Config.ARGB_8888, true));
            }
        } catch (Exception e) {
            toast("Ошибка: " + e.getMessage());
        }
    }

    private void bindRow(int rowId, boolean filters) {
        ViewGroup row = findViewById(rowId);
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            Object tag = child.getTag();
            if (tag == null) continue;
            final int index;
            try { index = Integer.parseInt(tag.toString()); } catch (Exception e) { continue; }
            child.setOnClickListener(v -> {
                if (bitmap == null) { toast("Сначала откройте фото"); return; }
                if (filters) {
                    setBitmap(ImageUtils.filter(bitmap, index));
                } else {
                    onTool(index);
                }
            });
        }
    }

    private void onTool(int index) {
        switch (index) {
            case 0: openAdjust(0); break;
            case 1: openAdjust(1); break;
            case 2: openAdjust(2); break;
            case 3: setBitmap(ImageUtils.rotate(bitmap, 90, false, false)); break;
            case 4: setBitmap(ImageUtils.rotate(bitmap, 0, true, false)); break;
            case 5: setBitmap(ImageUtils.rotate(bitmap, 0, false, true)); break;
            case 6: setBitmap(ImageUtils.cropCenter(bitmap, 1f)); break;
            case 7: setBitmap(ImageUtils.cropCenter(bitmap, 4f / 3f)); break;
            case 8: setBitmap(ImageUtils.cropCenter(bitmap, 16f / 9f)); break;
            case 9: askText(); break;
            case 10: askSticker(); break;
            case 11: startDrawing(); break;
            default: break;
        }
    }

    private void openAdjust(int mode) {
        adjustMode = mode;
        adjustBase = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        preview = adjustBase;
        String[] names = {"Яркость", "Контраст", "Насыщенность"};
        adjustTitle.setText(names[mode]);
        ((SeekBar) findViewById(R.id.adjustSeek)).setProgress(100);
        adjustPanel.setVisibility(View.VISIBLE);
    }

    private Bitmap makePreview(int value) {
        int b = 0, c = 0, s = 0;
        if (adjustMode == 0) b = value;
        else if (adjustMode == 1) c = value;
        else s = value;
        return ImageUtils.adjust(adjustBase, b, c, s);
    }

    private void startDrawing() {
        drawView.setBitmap(bitmap);
        drawView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        drawPanel.setVisibility(View.VISIBLE);
    }

    private void stopDrawing() {
        drawView.setVisibility(View.GONE);
        drawPanel.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(bitmap);
    }

    private void askText() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_text, null);
        EditText input = view.findViewById(R.id.textInput);
        Spinner spinner = view.findViewById(R.id.colorSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.text_colors));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        new AlertDialog.Builder(this)
                .setTitle("Добавить текст")
                .setView(view)
                .setPositiveButton("Добавить", (d, which) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) { toast("Введите текст"); return; }
                    int[] palette = {Color.WHITE, Color.BLACK, Color.RED, Color.GREEN,
                            Color.BLUE, Color.YELLOW, 0xFFE91E63};
                    addText(text, palette[Math.max(0, spinner.getSelectedItemPosition())]);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addText(String text, int color) {
        Bitmap out = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        float size = Math.max(28f, out.getWidth() / 12f);
        p.setTextSize(size);
        while (p.measureText(text) > out.getWidth() * 0.9f && size > 12f) {
            size *= 0.9f;
            p.setTextSize(size);
        }
        Paint outline = new Paint(p);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(Math.max(2f, size / 14f));
        outline.setColor(color == Color.WHITE ? Color.BLACK : Color.WHITE);
        float x = out.getWidth() / 2f, y = out.getHeight() / 2f;
        c.drawText(text, x, y, outline);
        c.drawText(text, x, y, p);
        setBitmap(out);
    }

    private void askSticker() {
        final String[] emojis = {"❤️", "😀", "🔥", "⭐", "🌈", "🐱", "🦄", "🎉",
                "✨", "🍕", "⚽", "🎵", "🌻", "💎", "🍀", "😎"};
        new AlertDialog.Builder(this)
                .setTitle("Выберите стикер")
                .setItems(emojis, (d, which) -> addSticker(emojis[which]))
                .show();
    }

    private void addSticker(String emoji) {
        Bitmap out = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(out.getWidth() / 5f);
        float x = out.getWidth() * (0.25f + (float) Math.random() * 0.5f);
        float y = out.getHeight() * (0.25f + (float) Math.random() * 0.5f);
        c.drawText(emoji, x, y, p);
        setBitmap(out);
    }

    private void setBitmap(Bitmap b) {
        if (b == null) return;
        bitmap = b;
        imageView.setImageBitmap(b);
        pushHistory(b);
    }

    private void pushHistory(Bitmap b) {
        Bitmap copy = b.copy(Bitmap.Config.ARGB_8888, true);
        while (history.size() - 1 > histPos && !history.isEmpty()) history.remove(history.size() - 1);
        history.add(copy);
        histPos = history.size() - 1;
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
            histPos--;
        }
    }

    private void showHistory(int pos) {
        histPos = pos;
        bitmap = history.get(pos).copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(bitmap);
    }

    private void undo() {
        if (histPos > 0) showHistory(histPos - 1); else toast("Больше нечего отменять");
    }

    private void redo() {
        if (histPos < history.size() - 1) showHistory(histPos + 1); else toast("Больше нечего повторить");
    }

    private Uri saveImage() {
        String name = "GreenBox_" + System.currentTimeMillis() + ".jpg";
        try {
            String saved = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, name, "GreenBox");
            if (saved != null) { toast("Сохранено в галерею"); return Uri.parse(saved); }
        } catch (Throwable ignored) { }
        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "GreenBox");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, name);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
            }
            toast("Сохранено: " + file.getAbsolutePath());
            return Uri.fromFile(file);
        } catch (Exception e) {
            toast("Не удалось сохранить: " + e.getMessage());
            return null;
        }
    }

    private void shareImage() {
        Uri uri = saveImage();
        if (uri == null) return;
        if (!"content".equals(uri.getScheme())) {
            toast("Поделиться можно после сохранения в галерею");
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/jpeg");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(i, "Поделиться фото"));
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
