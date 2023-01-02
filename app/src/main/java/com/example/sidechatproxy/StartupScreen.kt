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
import com.example.sidechatproxy.API_Handler.Companion.get_all_posts
import com.example.sidechatproxy.API_Handler.Companion.get_user_and_group
import com.example.sidechatproxy.LoadingScreen.Companion.setup_loading_screen
import java.util.concurrent.atomic.AtomicBoolean


class StartupScreen : AppCompatActivity() {
    companion object {
        //Probably bad practice, but I'm just using this companion object to store 'globals'
        var latest_errmsg: String = "Unknown Error!"
        var loading_complete: AtomicBoolean = AtomicBoolean(true)
        var loading_error: AtomicBoolean = AtomicBoolean(false)
        //var info_in_memory: MutableMap<String, Any> = mutableMapOf()
        var user_id: String? = null
        var group_id: String? = null
        var token: String? = null
        var memory_strings: MutableMap<String, String> = mutableMapOf()
        var memory_posts: MutableMap<String, List<Post>> = mutableMapOf()

        val load_everything_runnable = Runnable {
            if (token == null) {
                token = longterm_get("group_id")
                if (token == null) {
                    //If it's still null, error
                    latest_errmsg = "longterm stored token was null!"
                    loading_error.set(true)
                    return@Runnable
                }
            }
            if (group_id == null) {
                group_id = longterm_get("group_id")
                if (group_id == null) {
                    //If it's still null, error
                    latest_errmsg = "longterm stored group_id was null"
                    loading_error.set(true)
                    return@Runnable
                }
            }
            if (user_id == null) {
                Log.d("Debug", "Getting user/group in runnable")
                get_user_and_group()
            }
            if (memory_posts["hot"] == null || memory_posts["recent"] == null || memory_posts["top"] == null) {
                Log.d("Debug", "Getting posts in runnable")
                get_all_posts()
            }
            loading_complete.set(true)
        }

        @Suppress("StaticFieldLeak")
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
        user_id = longterm_get("user_id")
        group_id = longterm_get("group_id")

        //Attempt to retrieve token from memory to see if login exists
        token = longterm_get("token")

        val getStartedButton: Button = findViewById(R.id.get_started_button)
        val aboutButton: Button = findViewById(R.id.about_button)

        getStartedButton.setOnClickListener {
            if (token != null){
                Log.d("Debug", "Login already complete, going to PostMain")
                //Go straight to the posts
                setup_loading_screen(PostsMain::class.java, load_everything_runnable) //Setup loading screen
                startActivity(Intent(this, LoadingScreen::class.java)) //Go to loading screen
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