package com.greenbox.studio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnEditor).setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }
        findViewById<Button>(R.id.btnGames).setOnClickListener {
            startActivity(Intent(this, GamesActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
