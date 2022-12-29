package com.example.sidechatproxy

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg


class ErrorDisplay : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_error_display)
        Log.d("Debug", "Created Error Display msg: $latest_errmsg")
        val textView = findViewById<TextView>(R.id.error_content)
        textView.text = latest_errmsg
    }
}