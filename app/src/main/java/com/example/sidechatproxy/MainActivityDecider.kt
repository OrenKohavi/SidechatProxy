package com.example.sidechatproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class MainActivityDecider : AppCompatActivity() {
    companion object {
        val debug_mode: Boolean = true
        var latest_errmsg: String = "Unknown Error!"
        var info_in_memory: MutableMap<String, Any> = mutableMapOf()

        fun longterm_put(ctx: Activity, k: String, v: String) {
            val sharedPref = ctx.getPreferences(Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(k, v)
                apply()
            }
        }

        fun longterm_get(ctx: Activity, k: String): String? {
            val sharedPref = ctx.getPreferences(Context.MODE_PRIVATE)
            return sharedPref.getString(k, null)
        }
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