package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class MainActivityDecider : AppCompatActivity() {
    companion object {
        var latest_errmsg: String = "Unknown Error!"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Debug", "App Started")
        if (true) { //For now, just true.
            // Eventually, it should decide whether to do setup every time or not
            val switchActivityIntent = Intent(this, SetupPhone::class.java)
            startActivity(switchActivityIntent)
        }
    }
}