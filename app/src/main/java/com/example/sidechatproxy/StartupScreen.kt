package com.example.sidechatproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.API_Handler.Companion.get_all_posts
import com.example.sidechatproxy.API_Handler.Companion.get_user_and_group
import com.example.sidechatproxy.LoadingScreen.Companion.next_screen
import com.example.sidechatproxy.LoadingScreen.Companion.setup_loading_screen
import java.util.concurrent.atomic.AtomicBoolean

/*
TODOs:
Implement comment button
Implement upvotes/downvotes
FAQ Page
----- V0.1 Complete -----
Implement creating a text post [later, image posts as well]
Implement commenting
Implement poll viewing
Pre-load images when loading posts (in load_everything_runnable) -- Will prevent image pop-in
Make setup process more responsive[?]
 */

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
        var memory_strings: MutableMap<String, String?> = mutableMapOf()
        var memory_posts: MutableMap<String, List<Post>> = mutableMapOf()

        val load_everything_runnable = Runnable {
            if (token == null) {
                token = longterm_get("group_id")
                if (token == null) {
                    //If it's still null, setup needs to be re-done
                    //show_dialog(startup_activity_context, "Incomplete Setup", "Re-Doing Setup Process")
                    next_screen = SetupPhone::class.java
                    loading_complete.set(true)
                    return@Runnable
                }
            }
            if (group_id == null) {
                group_id = longterm_get("group_id")
                if (group_id == null) {
                    //If it's still null, setup needs to be redone
                    //We can resume from the 'email' screen, since a token already exists
                    //show_dialog(startup_activity_context, "Incomplete Setup", "Re-Doing Setup Process")
                    next_screen = SetupEmail::class.java
                    loading_complete.set(true)
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
            //TODO Go through the posts, and get all the images from all the posts before finishing loading!
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

        fun show_dialog(ctx: Context, title: String, message: String) {
            // setup the alert builder
            val builder: AlertDialog.Builder = AlertDialog.Builder(ctx)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK", null)
            val dialog: AlertDialog = builder.create()
            dialog.show()
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
        memory_strings["group_color"] = longterm_get("group_color")

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
            Toast.makeText(applicationContext, "FAQ Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }
}