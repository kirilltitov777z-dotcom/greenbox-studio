package com.greenbox.studio;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Game2048Activity extends Activity implements BoardView.Listener {

    private TextView scoreText;
    private BoardView board;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("2048");

        int p = GamesActivity.dp(14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        scoreText = new TextView(this);
        scoreText.setText("Очки: 0");
        scoreText.setTextSize(20);
        scoreText.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(scoreText);

        board = new BoardView(this);
        board.listener = this;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = p;
        lp.bottomMargin = p;
        board.setLayoutParams(lp);
        root.addView(board);

        Button restart = new Button(this);
        restart.setText("Новая игра");
        restart.setBackgroundResource(R.drawable.btn_green);
        restart.setTextColor(Color.WHITE);
        restart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        restart.setOnClickListener(v -> board.reset());
        root.addView(restart);

        TextView hint = new TextView(this);
        hint.setText("Свайпайте влево / вправо / вверх / вниз");
        hint.setTextSize(12);
        hint.setTextColor(Color.parseColor("#6B7F6B"));
        hint.setPadding(0, p, 0, 0);
        root.addView(hint);

        setContentView(root);
        board.reset();
    }

    @Override public void onScore(int score) {
        scoreText.setText("Очки: " + score);
    }

    @Override public void onLose(int score) {
        Toast.makeText(this, "Ходов больше нет! Очки: " + score, Toast.LENGTH_SHORT).show();
    }

    static class BoardView extends View implements View.OnTouchListener {

        interface Listener {
            void onScore(int score);
            void onLose(int score);
        }

        final int[][] cells = new int[4][4];
        int score = 0;
        boolean over = false;
        Listener listener;
        float downX, downY;
        final Random random = new Random();
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cellSize, gap, pad;

        BoardView(android.content.Context c) {
            super(c);
            setOnTouchListener(this);
            setBackgroundColor(Color.parseColor("#BBADA0"));
        }

        void reset() {
            for (int[] row : cells) Arrays.fill(row, 0);
            score = 0; over = false;
            addRandom(); addRandom();
            if (listener != null) listener.onScore(0);
            invalidate();
        }

        void addRandom() {
            List<int[]> empty = new ArrayList<>();
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    if (cells[r][c] == 0) empty.add(new int[]{r, c});
            if (empty.isEmpty()) return;
            int[] pick = empty.get(random.nextInt(empty.size()));
            cells[pick[0]][pick[1]] = random.nextDouble() < 0.9 ? 2 : 4;
        }

        int[] mergeLine(int[] line) {
            int[] tmp = new int[4];
            int k = 0;
            for (int v : line) if (v != 0) tmp[k++] = v;
            int[] out = new int[4];
            int j = 0;
            for (int i = 0; i < k; i++) {
                if (i + 1 < k && tmp[i] == tmp[i + 1]) {
                    out[j++] = tmp[i] * 2;
                    score += tmp[i] * 2;
                    i++;
                } else {
                    out[j++] = tmp[i];
                }
            }
            return out;
        }

        void move(int dr, int dc) {
            if (over) return;
            boolean changed = false;
            if (dc != 0) {
                for (int r = 0; r < 4; r++) {
                    int[] line = new int[4];
                    for (int c = 0; c < 4; c++) line[c] = cells[r][dc < 0 ? c : 3 - c];
                    int[] merged = mergeLine(line);
                    for (int c = 0; c < 4; c++) cells[r][dc < 0 ? c : 3 - c] = merged[c];
                    if (!Arrays.equals(line, merged)) changed = true;
                }
            } else {
                for (int c = 0; c < 4; c++) {
                    int[] line = new int[4];
                    for (int r = 0; r < 4; r++) line[r] = cells[dr < 0 ? r : 3 - r][c];
                    int[] merged = mergeLine(line);
                    for (int r = 0; r < 4; r++) cells[dr < 0 ? r : 3 - r][c] = merged[r];
                    if (!Arrays.equals(line, merged)) changed = true;
                }
            }
            if (!changed) return;
            addRandom();
            if (listener != null) listener.onScore(score);
            if (isStuck()) {
                over = true;
                if (listener != null) listener.onLose(score);
            }
            invalidate();
        }

        boolean isStuck() {
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++) {
                    if (cells[r][c] == 0) return false;
                    if (c < 3 && cells[r][c] == cells[r][c + 1]) return false;
                    if (r < 3 && cells[r][c] == cells[r + 1][c]) return false;
                }
            return true;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            gap = GamesActivity.dp(6);
            pad = GamesActivity.dp(6);
            cellSize = (Math.min(w, h) - pad * 2 - gap * 3) / 4f;
        }

        int colorFor(int v) {
            switch (v) {
                case 2: return 0xFFEEE4DA;
                case 4: return 0xFFEDE0C8;
                case 8: return 0xFFF2B179;
                case 16: return 0xFFF59563;
                case 32: return 0xFFF67C5F;
                case 64: return 0xFFF65E3B;
                case 128: return 0xFFEDCF72;
                case 256: return 0xFFEDCC61;
                case 512: return 0xFFEDC850;
                case 1024: return 0xFFEDC53F;
                default: return 0xFF3C3A32;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            for (int r = 0; r < 4; r++) {
                for (int col = 0; col < 4; col++) {
                    float x = pad + col * (cellSize + gap);
                    float y = pad + r * (cellSize + gap);
                    int v = cells[r][col];
                    paint.setColor(v == 0 ? 0x66FFFFFF : colorFor(v));
                    c.drawRoundRect(new RectF(x, y, x + cellSize, y + cellSize),
                            GamesActivity.dp(6), GamesActivity.dp(6), paint);
                    if (v != 0) {
                        paint.setColor(v >= 8 ? Color.WHITE : Color.parseColor("#776E65"));
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setTextSize(cellSize * (v >= 1024 ? 0.28f : 0.42f));
                        c.drawText(String.valueOf(v), x + cellSize / 2,
                                y + cellSize / 2 + paint.getTextSize() * 0.35f, paint);
                    }
                }
            }
            if (over) {
                paint.setColor(Color.parseColor("#99000000"));
                c.drawRect(0, 0, getWidth(), getHeight(), paint);
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(Math.max(26, cellSize * 0.7f));
                c.drawText("Конец игры", getWidth() / 2f, getHeight() / 2f, paint);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downX = e.getX(); downY = e.getY();
            } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) < 20) return true;
                if (Math.abs(dx) > Math.abs(dy)) move(0, dx > 0 ? 1 : -1);
                else move(dy > 0 ? 1 : -1, 0);
            }
            return true;
        }
    }
}
