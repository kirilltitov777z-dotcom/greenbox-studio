package com.greenbox.studio;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryActivity extends Activity {

    private static final String[] EMOJIS = {
            "🍎", "🍌", "🍇", "🍓", "🐱", "🐶", "🚗", "⭐"
    };

    private final List<String> deck = new ArrayList<>();
    private final Button[] cards = new Button[16];
    private final boolean[] revealed = new boolean[16];
    private final boolean[] matched = new boolean[16];
    private int first = -1, second = -1, moves = 0, pairs = 0;
    private boolean busy = false;
    private TextView info;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Память");

        int p = GamesActivity.dp(12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        info = new TextView(this);
        info.setTextSize(18);
        info.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(info);

        for (int row = 0; row < 4; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 4; col++) {
                final int pos = row * 4 + col;
                Button b = new Button(this);
                b.setText("❓");
                b.setTextSize(26);
                b.setBackgroundResource(R.drawable.btn_soft);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        GamesActivity.dp(70), 1f);
                lp.setMargins(GamesActivity.dp(3), GamesActivity.dp(3),
                        GamesActivity.dp(3), GamesActivity.dp(3));
                b.setLayoutParams(lp);
                b.setOnClickListener(v -> flip(pos));
                cards[pos] = b;
                line.addView(b);
            }
            root.addView(line);
        }

        Button restart = new Button(this);
        restart.setText("Заново");
        restart.setBackgroundResource(R.drawable.btn_green);
        restart.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = p;
        restart.setLayoutParams(rp);
        restart.setOnClickListener(v -> newGame());
        root.addView(restart);

        setContentView(root);
        newGame();
    }

    private void newGame() {
        deck.clear();
        for (String e : EMOJIS) { deck.add(e); deck.add(e); }
        Collections.shuffle(deck);
        for (int i = 0; i < 16; i++) {
            revealed[i] = false;
            matched[i] = false;
            cards[i].setText("❓");
        }
        first = second = -1;
        moves = 0; pairs = 0; busy = false;
        updateInfo();
    }

    private void updateInfo() {
        info.setText("Ходы: " + moves + " · Пары: " + pairs + "/8");
    }

    private void flip(int pos) {
        if (busy || matched[pos] || revealed[pos]) return;
        revealed[pos] = true;
        cards[pos].setText(deck.get(pos));
        if (first == -1) {
            first = pos;
            return;
        }
        second = pos;
        moves++;
        updateInfo();
        busy = true;
        handler.postDelayed(() -> {
            if (deck.get(first).equals(deck.get(second))) {
                matched[first] = true;
                matched[second] = true;
                pairs++;
                updateInfo();
                first = second = -1;
                busy = false;
                if (pairs == 8) Toast.makeText(this, "Все пары найдены! 🎉", Toast.LENGTH_SHORT).show();
            } else {
                handler.postDelayed(() -> {
                    cards[first].setText("❓");
                    cards[second].setText("❓");
                    revealed[first] = false;
                    revealed[second] = false;
                    first = second = -1;
                    busy = false;
                }, 450);
            }
        }, 650);
    }
}
