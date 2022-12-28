package com.example.sidechatproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class StartupScreen : AppCompatActivity() {
    companion object {
        //Probably bad practice, but I'm just using this companion object to store 'globals'
        const val debug_mode: Boolean = true
        var latest_errmsg: String = "Unknown Error!"
        var info_in_memory: MutableMap<String, Any> = mutableMapOf()
        lateinit var startup_activity_context: Activity
        // ^ Yes, this is a memory leak because the StartupScreen will never get deallocated
        // ^ I'm still using it because it's easy,
        // ^ and because the startup activity is simple and so it's no big deal

        fun longterm_put(k: String, v: String) {
            val sharedPref = startup_activity_context.getPreferences(Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(k, v)
                apply()
            }
        }

        fun longterm_get(k: String): String? {
            val sharedPref = startup_activity_context.getPreferences(Context.MODE_PRIVATE)
            return sharedPref.getString(k, null)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_startup)
        //Setup stuff
        Log.d("Debug", "App Started")
        startup_activity_context = this
        info_in_memory["user_stored"] = false
        info_in_memory["group_stored"] = false
        info_in_memory["posts_stored"] = false

        //Attempt to retrieve token from memory to see if login exists
        val token = longterm_get("token")
        info_in_memory["login_complete"] = (token != null)

        val getStartedButton: Button = findViewById(R.id.get_started_button)
        val aboutButton: Button = findViewById(R.id.about_button)

        getStartedButton.setOnClickListener {
            if (info_in_memory["login_complete"] == true){
                Log.d("Debug", "Login already complete, going to PostMain")
                //Go straight to the posts
                val switchActivityIntent = Intent(this, PostsMain::class.java)
                startActivity(switchActivityIntent)
            } else {
                Log.d("Debug", "Login not complete, going to SetupPhone")
                //Go to the login process
                val switchActivityIntent = Intent(this, SetupPhone::class.java)
                startActivity(switchActivityIntent)
            }
        }

        aboutButton.setOnClickListener {
            Toast.makeText(applicationContext, "Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }
}