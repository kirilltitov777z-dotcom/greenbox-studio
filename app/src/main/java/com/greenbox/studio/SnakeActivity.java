package com.greenbox.studio;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class SnakeActivity extends Activity implements SnakeView.Listener {

    private TextView scoreText;
    private SnakeView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Змейка");

        int p = GamesActivity.dp(12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        scoreText = new TextView(this);
        scoreText.setText("Очки: 0");
        scoreText.setTextSize(18);
        scoreText.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(scoreText);

        game = new SnakeView(this);
        game.listener = this;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = p;
        lp.bottomMargin = p;
        game.setLayoutParams(lp);
        root.addView(game);

        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.HORIZONTAL);
        pad.setGravity(android.view.Gravity.CENTER);
        int[][] dirs = {{0,-1}, {-1,0}, {1,0}, {0,1}};
        String[] labels = {"↑", "←", "→", "↓"};
        for (int i = 0; i < 4; i++) {
            final int dx = dirs[i][0], dy = dirs[i][1];
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(18);
            b.setBackgroundResource(R.drawable.btn_soft);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    GamesActivity.dp(56), GamesActivity.dp(48));
            bp.leftMargin = GamesActivity.dp(4);
            b.setLayoutParams(bp);
            b.setOnClickListener(v -> game.setDirection(dx, dy));
            pad.addView(b);
        }
        root.addView(pad);

        Button restart = new Button(this);
        restart.setText("Новая игра");
        restart.setBackgroundResource(R.drawable.btn_green);
        restart.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = p;
        restart.setLayoutParams(rp);
        restart.setOnClickListener(v -> game.reset());
        root.addView(restart);

        setContentView(root);
        game.reset();
    }

    @Override protected void onResume() { super.onResume(); game.resume(); }
    @Override protected void onPause() { super.onPause(); game.pause(); }

    @Override public void onScore(int score) {
        scoreText.setText("Очки: " + score);
    }

    @Override public void onGameOver(int score) {
        scoreText.setText("Игра окончена · очки: " + score);
        Toast.makeText(this, "Игра окончена!", Toast.LENGTH_SHORT).show();
    }

    static class SnakeView extends View implements View.OnTouchListener {

        interface Listener {
            void onScore(int score);
            void onGameOver(int score);
        }

        static final int N = 20;
        Listener listener;
        final ArrayList<Point> snake = new ArrayList<>();
        Point food;
        int dirX = 1, dirY = 0, score = 0;
        boolean running = false, over = false;
        float cell = 20f, ox = 0f, oy = 0f;
        float downX, downY;
        final Random random = new Random();
        final Handler handler = new Handler();
        final Runnable tick = () -> {
            if (!running) return;
            step();
            if (running) handler.postDelayed(tick, 120);
        };

        SnakeView(android.content.Context c) {
            super(c);
            setOnTouchListener(this);
            setBackgroundColor(Color.parseColor("#102A15"));
        }

        void reset() {
            snake.clear();
            int cy = N / 2;
            for (int i = 3; i >= 0; i--) snake.add(new Point(N / 2 - i, cy));
            dirX = 1; dirY = 0;
            score = 0;
            over = false;
            if (listener != null) listener.onScore(0);
            spawnFood();
            running = true;
            handler.removeCallbacks(tick);
            handler.post(tick);
            invalidate();
        }

        void resume() {
            if (!over && !running) {
                running = true;
                handler.removeCallbacks(tick);
                handler.post(tick);
            }
        }

        void pause() {
            running = false;
            handler.removeCallbacks(tick);
        }

        void setDirection(int dx, int dy) {
            if (dirX == -dx && dirY == -dy && snake.size() > 1) return;
            dirX = dx; dirY = dy;
        }

        void spawnFood() {
            for (int i = 0; i < 500; i++) {
                Point p = new Point(random.nextInt(N), random.nextInt(N));
                if (!snake.contains(p)) { food = p; return; }
            }
            food = new Point(0, 0);
        }

        void step() {
            Point head = snake.get(0);
            Point next = new Point(head.x + dirX, head.y + dirY);
            if (next.x < 0 || next.x >= N || next.y < 0 || next.y >= N || snake.contains(next)) {
                over = true;
                running = false;
                if (listener != null) listener.onGameOver(score);
                invalidate();
                return;
            }
            snake.add(0, next);
            if (food != null && next.equals(food)) {
                score++;
                if (listener != null) listener.onScore(score);
                spawnFood();
            } else {
                snake.remove(snake.size() - 1);
            }
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            cell = Math.min(w, h) / (float) N;
            ox = (w - cell * N) / 2f;
            oy = (h - cell * N) / 2f;
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            Paint grid = new Paint();
            grid.setColor(Color.parseColor("#1C4224"));
            grid.setStrokeWidth(1f);
            for (int i = 0; i <= N; i++) {
                c.drawLine(ox + i * cell, oy, ox + i * cell, oy + N * cell, grid);
                c.drawLine(ox, oy + i * cell, ox + N * cell, oy + i * cell, grid);
            }
            if (food != null) {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.parseColor("#FF5252"));
                c.drawCircle(ox + food.x * cell + cell / 2, oy + food.y * cell + cell / 2,
                        cell * 0.36f, p);
            }
            for (int i = snake.size() - 1; i >= 0; i--) {
                Point pt = snake.get(i);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(i == 0 ? Color.parseColor("#AEDD6A") : Color.parseColor("#4CAF50"));
                RectF r = new RectF(ox + pt.x * cell + 1, oy + pt.y * cell + 1,
                        ox + (pt.x + 1) * cell - 1, oy + (pt.y + 1) * cell - 1);
                c.drawRoundRect(r, cell * 0.25f, cell * 0.25f, p);
            }
            if (over) {
                Paint shade = new Paint();
                shade.setColor(Color.parseColor("#AA000000"));
                c.drawRect(0, 0, getWidth(), getHeight(), shade);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.WHITE);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(Math.max(24, cell * 2));
                c.drawText("Игра окончена", getWidth() / 2f, getHeight() / 2f, p);
                p.setTextSize(Math.max(16, cell));
                c.drawText("Очки: " + score + " · «Новая игра»",
                        getWidth() / 2f, getHeight() / 2f + cell * 2, p);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downX = e.getX(); downY = e.getY();
            } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) < 20) return true;
                if (Math.abs(dx) > Math.abs(dy)) setDirection(dx > 0 ? 1 : -1, 0);
                else setDirection(0, dy > 0 ? 1 : -1);
            }
            return true;
        }
    }
}
