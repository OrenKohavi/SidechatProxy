package com.example.sidechatproxy

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.TextKeyListener.clear
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class StartupScreen : AppCompatActivity() {
    companion object {
        //Probably bad practice, but I'm just using this companion object to store 'globals'
        var latest_errmsg: String = "Unknown Error!"
        var info_in_memory: MutableMap<String, Any> = mutableMapOf()
        @SuppressLint("StaticFieldLeak")
        lateinit var startup_activity_context: Activity
        // ^ Yes, this is a memory leak because the StartupScreen will never get deallocated
        // ^ I'm still using it because it's easy,
        // ^ and the startup activity is simple and so it's no big deal (not too much memory leaking)

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
        Log.d("Debug", "Set Startup Context")
        //Attempt to retrieve user_id
        val user_id: String? = longterm_get("user_id")
        if (user_id != null){
            info_in_memory["user_stored"] = true
            info_in_memory["user_id"] = user_id
        } else {
            info_in_memory["user_stored"] = false
        }
        val group_id: String? = longterm_get("group_id")
        if (group_id != null){
            info_in_memory["group_id_stored"] = true
            info_in_memory["group_id"] = group_id
        } else {
            info_in_memory["group_id_stored"] = false
        }
        info_in_memory["posts_stored"] = false

        //Attempt to retrieve token from memory to see if login exists
        val token: String? = longterm_get("token")
        if (token != null){
            info_in_memory["login_complete"] = true
            info_in_memory["token"] = token
        } else {
            info_in_memory["login_complete"] = false
        }

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