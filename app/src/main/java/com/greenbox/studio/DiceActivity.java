package com.greenbox.studio;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class DiceActivity extends Activity {

    private static final String[] FACES = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};
    private final Random random = new Random();
    private final Handler handler = new Handler();
    private int count = 2;
    private boolean rolling = false;
    private TextView[] diceViews;
    private TextView sumText;
    private Button rollButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Кубики");

        int p = GamesActivity.dp(14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("Сколько кубиков бросить?");
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(title);

        LinearLayout picker = new LinearLayout(this);
        picker.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 1; i <= 6; i++) {
            final int n = i;
            Button b = new Button(this);
            b.setText(String.valueOf(n));
            b.setBackgroundResource(R.drawable.btn_soft);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    GamesActivity.dp(44), 1f);
            lp.setMargins(GamesActivity.dp(2), GamesActivity.dp(8), GamesActivity.dp(2), 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                count = n;
                buildDice();
            });
            picker.addView(b);
        }
        root.addView(picker);

        LinearLayout diceRow = new LinearLayout(this);
        diceRow.setOrientation(LinearLayout.HORIZONTAL);
        diceRow.setGravity(Gravity.CENTER);
        diceRow.setMinimumHeight(GamesActivity.dp(140));
        diceRow.setId(0x7F010001);
        LinearLayout.LayoutParams dr = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, GamesActivity.dp(150));
        dr.topMargin = GamesActivity.dp(12);
        diceRow.setLayoutParams(dr);
        root.addView(diceRow);

        sumText = new TextView(this);
        sumText.setText("Сумма: —");
        sumText.setTextSize(20);
        sumText.setGravity(Gravity.CENTER);
        sumText.setTextColor(Color.parseColor("#1B2B1B"));
        root.addView(sumText);

        rollButton = new Button(this);
        rollButton.setText("🎲 Бросить");
        rollButton.setBackgroundResource(R.drawable.btn_green);
        rollButton.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = p;
        rollButton.setLayoutParams(rp);
        rollButton.setOnClickListener(v -> roll());
        root.addView(rollButton);

        setContentView(root);
        buildDice();
    }

    private void buildDice() {
        LinearLayout row = findViewById(0x7F010001);
        row.removeAllViews();
        diceViews = new TextView[count];
        for (int i = 0; i < count; i++) {
            TextView t = new TextView(this);
            t.setText("⚀");
            t.setTextSize(56);
            t.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(GamesActivity.dp(2), 0, GamesActivity.dp(2), 0);
            t.setLayoutParams(lp);
            t.setBackgroundResource(R.drawable.bg_card);
            diceViews[i] = t;
            row.addView(t);
        }
        sumText.setText("Сумма: —");
    }

    private void roll() {
        if (rolling) return;
        rolling = true;
        rollButton.setEnabled(false);
        final int[] steps = {0};
        Handler.Callback cb = null;
        final Runnable animate = new Runnable() {
            @Override public void run() {
                int sum = 0;
                for (int i = 0; i < diceViews.length; i++) {
                    int value = random.nextInt(6);
                    diceViews[i].setText(FACES[value]);
                    sum += value + 1;
                }
                sumText.setText("Сумма: " + sum);
                steps[0]++;
                if (steps[0] < 12) handler.postDelayed(this, 70);
                else {
                    rolling = false;
                    rollButton.setEnabled(true);
                }
            }
        };
        handler.post(animate);
    }
}
