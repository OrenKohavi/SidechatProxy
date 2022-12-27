package com.example.sidechatproxy

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class ErrorDisplay : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_error_display)
        Log.d("Debug", "Created Error Display")
        val textView = findViewById<TextView>(R.id.error_content)
        textView.text = MainActivityDecider.latest_errmsg
    }
}