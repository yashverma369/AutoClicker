package com.example.autoclicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = TextView(this).apply {
            text = "1. Neeche button dabao\n2. Installed apps / Downloaded apps me 'Simple AutoClicker' kholo\n3. ON karo\n\nRed dot aur panel dikhega. Kaam ke baad Close dabao, service OFF ho jayegi."
            textSize = 16f
            setPadding(0, 0, 0, 48)
        }
        val b = Button(this).apply {
            text = "Accessibility settings kholo"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(info); addView(b)
        }
        setContentView(root)
    }
}
