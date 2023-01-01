package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.StartupScreen.Companion.loading_complete
import com.example.sidechatproxy.StartupScreen.Companion.next_screen

class LoadingScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_loading)
        Log.d("Debug", "Loading Screen Created: Loading Complete?: $loading_complete")
        //Wait until loading_complete flag is set, then transition to the next_screen
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                if (loading_complete) {
                    val switchActivityIntent = Intent(next_screen, PostsMain::class.java)
                    startActivity(switchActivityIntent)
                } else {
                    //try again in 100ms
                    mainHandler.postDelayed(this, 100)
                }
            }
        })
    }
}