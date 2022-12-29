package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.sidechatproxy.API_Handler.Companion.get_all_posts
import com.example.sidechatproxy.API_Handler.Companion.get_user_and_group
import com.example.sidechatproxy.MyFragmentStateAdapter.Companion.page_to_category
import com.example.sidechatproxy.StartupScreen.Companion.info_in_memory
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MyFragmentStateAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    companion object {
        val page_to_category: Map<Int, String> = mapOf(0 to "hot", 1 to "new", 2 to "top")
    }

    override fun getItemCount() = 3 //Hardcoded 3 pages (Hot, New, Top)

    override fun createFragment(position: Int): Fragment {
        val category: String = page_to_category[position] ?: throw IllegalStateException("Page position was $position -- Illegal!")
        return fragment_post_list.newInstance(category)
    }
}

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
        if (!(info_in_memory["group_id_stored"] as Boolean)) {
            Log.d("Debug", "Group_id was not loaded in PostsMain -- It should be")
            latest_errmsg = "Group_id not loaded in PostsMain:\n memory data: $info_in_memory"
            val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
            startActivity(switchActivityIntent)
            return
        }
        //User and Group are loaded, so we can fetch posts!
        if (!(info_in_memory["posts_stored"] as Boolean)) {
            try {
                get_all_posts()
            } catch (e: APIException) {
                StartupScreen.latest_errmsg = e.message.toString()
                val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
                startActivity(switchActivityIntent)
            }
        }
        //Setup tabs
        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)

        val adapter = MyFragmentStateAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(
            tabLayout, viewPager
        ) { tab, position ->
            tab.text = page_to_category.getOrDefault(position, "Unknown").replaceFirstChar { it.uppercaseChar() }
        }.attach()

    }
}