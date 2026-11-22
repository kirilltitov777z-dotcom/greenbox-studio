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
import java.util.List;
import java.util.Random;

public class TicTacToeActivity extends Activity {

    private final char[] board = new char[9];
    private final Button[] buttons = new Button[9];
    private TextView status, scoreLine;
    private boolean playerTurn = true, finished = false;
    private int wins = 0, losses = 0, draws = 0;
    private final Handler handler = new Handler();
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Крестики-нолики");

        int p = GamesActivity.dp(14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        status = new TextView(this);
        status.setText("Вы — X. Ваш ход!");
        status.setTextSize(20);
        status.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(status);

        scoreLine = new TextView(this);
        scoreLine.setText("Победы: 0 · Поражения: 0 · Ничьи: 0");
        scoreLine.setTextSize(13);
        scoreLine.setTextColor(Color.parseColor("#6B7F6B"));
        scoreLine.setPadding(0, GamesActivity.dp(4), 0, GamesActivity.dp(14));
        root.addView(scoreLine);

        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 3; col++) {
                final int pos = row * 3 + col;
                Button b = new Button(this);
                b.setText("·");
                b.setTextSize(30);
                b.setBackgroundResource(R.drawable.btn_soft);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        GamesActivity.dp(80), 1f);
                lp.setMargins(GamesActivity.dp(3), GamesActivity.dp(3),
                        GamesActivity.dp(3), GamesActivity.dp(3));
                b.setLayoutParams(lp);
                b.setOnClickListener(v -> onCell(pos));
                buttons[pos] = b;
                line.addView(b);
            }
            root.addView(line);
        }

        Button restart = new Button(this);
        restart.setText("Новая партия");
        restart.setBackgroundResource(R.drawable.btn_green);
        restart.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = GamesActivity.dp(16);
        restart.setLayoutParams(rp);
        restart.setOnClickListener(v -> newRound());
        root.addView(restart);

        setContentView(root);
        newRound();
    }

    private void newRound() {
        for (int i = 0; i < 9; i++) { board[i] = ' '; buttons[i].setText("·"); }
        playerTurn = true;
        finished = false;
        status.setText("Вы — X. Ваш ход!");
    }

    private void onCell(int pos) {
        if (finished || !playerTurn || board[pos] != ' ') return;
        board[pos] = 'X';
        buttons[pos].setText("X");
        buttons[pos].setTextColor(Color.parseColor("#2E7D32"));
        if (finishIfNeeded()) return;
        playerTurn = false;
        status.setText("Компьютер думает...");
        handler.postDelayed(this::aiMove, 350);
    }

    private void aiMove() {
        if (finished) return;
        int best = -1000;
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] != ' ') continue;
            board[i] = 'O';
            int score = minimax(false);
            board[i] = ' ';
            if (score > best) { best = score; candidates.clear(); candidates.add(i); }
            else if (score == best) candidates.add(i);
        }
        if (candidates.isEmpty()) return;
        int move = candidates.get(random.nextInt(candidates.size()));
        board[move] = 'O';
        buttons[move].setText("O");
        buttons[move].setTextColor(Color.parseColor("#E65100"));
        if (!finishIfNeeded()) {
            playerTurn = true;
            status.setText("Ваш ход!");
        }
    }

    private int evaluate() {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] l : lines) {
            char a = board[l[0]], b = board[l[1]], c = board[l[2]];
            if (a != ' ' && a == b && a == c) return a == 'X' ? -10 : 10;
        }
        for (char cell : board) if (cell == ' ') return 0;
        return 2; // draw marker, handled below
    }

    private int minimax(boolean maximizing) {
        int res = evaluate();
        if (res == -10 || res == 10) return res;
        if (res == 2) return 0;
        int best = maximizing ? -1000 : 1000;
        for (int i = 0; i < 9; i++) {
            if (board[i] != ' ') continue;
            board[i] = maximizing ? 'O' : 'X';
            int s = minimax(!maximizing);
            board[i] = ' ';
            best = maximizing ? Math.max(best, s) : Math.min(best, s);
        }
        return best;
    }

    private boolean finishIfNeeded() {
        int res = evaluate();
        if (res == 0) return false;
        finished = true;
        if (res == -10) {
            wins++;
            status.setText("Вы победили! 🎉");
            Toast.makeText(this, "Победа! 🎉", Toast.LENGTH_SHORT).show();
        } else if (res == 10) {
            losses++;
            status.setText("Компьютер победил!");
            Toast.makeText(this, "Вы проиграли 😔", Toast.LENGTH_SHORT).show();
        } else {
            draws++;
            status.setText("Ничья");
        }
        scoreLine.setText("Победы: " + wins + " · Поражения: " + losses + " · Ничьи: " + draws);
        return true;
    }
}
