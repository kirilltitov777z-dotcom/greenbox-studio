package com.greenbox.studio;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class ReactionActivity extends Activity {

    private TextView status, bestText;
    private Button target, start;
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private boolean waiting = false;
    private long startTime = 0;
    private int best = Integer.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Реакция");

        int p = GamesActivity.dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        status = new TextView(this);
        status.setText("Нажмите «Старт» и ждите сигнала");
        status.setTextSize(18);
        status.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(status);

        bestText = new TextView(this);
        bestText.setText("Лучший результат: —");
        bestText.setTextSize(14);
        bestText.setTextColor(Color.parseColor("#6B7F6B"));
        bestText.setPadding(0, GamesActivity.dp(6), 0, GamesActivity.dp(20));
        root.addView(bestText);

        target = new Button(this);
        target.setText("ЖДИ...");
        target.setTextSize(28);
        target.setTextColor(Color.WHITE);
        target.setBackgroundColor(Color.parseColor("#9E9E9E"));
        target.setEnabled(false);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        target.setLayoutParams(tp);
        target.setOnClickListener(v -> onTarget());
        root.addView(target);

        start = new Button(this);
        start.setText("Старт");
        start.setBackgroundResource(R.drawable.btn_green);
        start.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = p;
        start.setLayoutParams(sp);
        start.setOnClickListener(v -> startRound());
        root.addView(start);

        setContentView(root);
    }

    private void startRound() {
        handler.removeCallbacksAndMessages(null);
        waiting = false;
        start.setEnabled(false);
        target.setEnabled(false);
        target.setText("ЖДИ...");
        target.setBackgroundColor(Color.parseColor("#9E9E9E"));
        status.setText("Ждите зелёного сигнала...");
        int delay = 1500 + random.nextInt(2500);
        handler.postDelayed(() -> {
            waiting = true;
            startTime = System.nanoTime();
            target.setText("ЖМИ!");
            target.setBackgroundColor(Color.parseColor("#43A047"));
            target.setEnabled(true);
            status.setText("ЖМИ!");
        }, delay);
    }

    private void onTarget() {
        if (!waiting) return;
        long elapsed = Math.round((System.nanoTime() - startTime) / 1_000_000f);
        waiting = false;
        target.setEnabled(false);
        target.setBackgroundColor(Color.parseColor("#9E9E9E"));
        target.setText("Твой ход!");
        status.setText("Реакция: " + elapsed + " мс");
        if (elapsed < best) {
            best = elapsed;
            bestText.setText("Лучший результат: " + best + " мс");
        }
        start.setEnabled(true);
    }
}
