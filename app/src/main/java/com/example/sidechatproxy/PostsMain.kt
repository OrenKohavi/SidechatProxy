package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.API_Handler.Companion.get_all_posts
import com.example.sidechatproxy.API_Handler.Companion.get_user_and_group
import com.example.sidechatproxy.StartupScreen.Companion.info_in_memory
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg

class PostsMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_posts_main)
        //Make sure that the user, group, and posts are all loaded
        if (!(info_in_memory["user_stored"] as Boolean)) {
            //Load the user (and group, because they come as part of the same API response)
            try {
                get_user_and_group()
            } catch (e : APIException) {
                latest_errmsg = e.message.toString()
                val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
                startActivity(switchActivityIntent)
            }
        }
        if (!(info_in_memory["group_stored"] as Boolean)) {
            Log.d("Debug", "Group was not loaded in PostsMain -- It should be")
            latest_errmsg = "Group not loaded in PostsMain:\n memory data: $info_in_memory"
            val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
            startActivity(switchActivityIntent)
            return
        }
        //User and Group are loaded, so we can fetch posts!
        get_all_posts()
    }
}