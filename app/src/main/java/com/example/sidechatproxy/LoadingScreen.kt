package com.example.sidechatproxy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg
import com.example.sidechatproxy.StartupScreen.Companion.loading_complete
import com.example.sidechatproxy.StartupScreen.Companion.loading_error
import kotlinx.coroutines.Runnable
import java.util.concurrent.atomic.AtomicBoolean

class LoadingScreen : AppCompatActivity() {
    companion object {
        var next_screen: Class<*>? = null

        fun setup_loading_screen(next_screen_arg: Class<*>, loading_runnable: Runnable) {
            //Start the runnable in a new thread
            loading_complete.set(false)
            val thread = Thread(loading_runnable)
            thread.start()
            //set next_screen
            next_screen = next_screen_arg
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_loading)
        Log.d("Debug", "Loading Screen Created: Loading Complete?: $loading_complete")
        //Wait until loading_complete flag is set, then transition to the next_screen
        if (next_screen == null) {
            Log.d("Debug", "Next Screen is Null! Raising Error")
            next_screen = ErrorDisplay::class.java
            latest_errmsg = "No next_screen for loading!"
            loading_complete.set(true)
        }
        val mainHandler = Handler(Looper.getMainLooper())
        val ctx = this
        mainHandler.post(object : Runnable {
            override fun run() {
                if (loading_error.get()) {
                    Log.d("Debug", "Loading error encountered!")
                    startActivity(Intent(ctx, ErrorDisplay::class.java))
                }
                else if (loading_complete.get()) {
                    val switchActivityIntent = Intent(ctx, next_screen)
                    startActivity(switchActivityIntent)
                } else {
                    //try again in 25ms
                    mainHandler.postDelayed(this, 25)
                }
            }
        })
    }
}