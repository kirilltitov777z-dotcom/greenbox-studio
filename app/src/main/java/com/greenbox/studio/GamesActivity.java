package com.greenbox.studio;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class GamesActivity extends Activity {

    private static final String[] NAMES = {
            "🐍  Змейка", "🔢  2048", "❌⭕  Крестики-нолики",
            "🧠  Память", "🎲  Кубики", "⚡  Реакция"
    };
    private static final String[] DESCS = {
            "Ешьте яблоки и не врезайтесь в стену",
            "Сливайте плитки и соберите 2048",
            "Сразитесь с компьютером",
            "Найдите 8 пар карточек",
            "Бросайте от 1 до 6 кубиков",
            "Нажмите как можно быстрее"
    };
    private static final Class<?>[] CLASSES = {
            SnakeActivity.class, Game2048Activity.class, TicTacToeActivity.class,
            MemoryActivity.class, DiceActivity.class, ReactionActivity.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Игры");

        int pad = dp(18);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("🎮 Mini-игры");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#2E7D32"));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Выберите игру — все работают офлайн");
        sub.setTextSize(14);
        sub.setTextColor(Color.parseColor("#6B7F6B"));
        sub.setPadding(0, dp(4), 0, dp(18));
        root.addView(sub);

        for (int i = 0; i < NAMES.length; i++) {
            final Class<?> target = CLASSES[i];
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(12);
            card.setLayoutParams(lp);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setOnClickListener(v -> startActivity(new Intent(this, target)));

            TextView name = new TextView(this);
            name.setText(NAMES[i]);
            name.setTextSize(20);
            name.setTextColor(Color.parseColor("#1B2B1B"));
            card.addView(name);

            TextView desc = new TextView(this);
            desc.setText(DESCS[i]);
            desc.setTextSize(13);
            desc.setTextColor(Color.parseColor("#6B7F6B"));
            desc.setPadding(0, dp(4), 0, 0);
            card.addView(desc);

            TextView play = new TextView(this);
            play.setText("Играть →");
            play.setTextSize(13);
            play.setTextColor(Color.parseColor("#43A047"));
            play.setGravity(Gravity.END);
            play.setPadding(0, dp(8), 0, 0);
            card.addView(play);

            root.addView(card);
        }

        scroll.addView(root);
        setContentView(scroll);
    }

    public static int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                Resources.getSystem().getDisplayMetrics()));
    }
}
