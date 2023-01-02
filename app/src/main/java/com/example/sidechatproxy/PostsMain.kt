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
import com.example.sidechatproxy.StartupScreen.Companion.group_id
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg
import com.example.sidechatproxy.StartupScreen.Companion.memory_posts
import com.example.sidechatproxy.StartupScreen.Companion.token
import com.example.sidechatproxy.StartupScreen.Companion.user_id
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MyFragmentStateAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    companion object {
        val page_to_category: Map<Int, String> = mapOf(0 to "hot", 1 to "recent", 2 to "top")
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
        if (user_id == null || group_id == null || token == null) {
            Log.d("Debug", "Null core values in PostsMain! $user_id | $group_id | $token")
            latest_errmsg = "Null core values in PostsMain! $user_id | $group_id | $token"
            startActivity(Intent(this, ErrorDisplay::class.java))
        }
        //User and Group are loaded, so we can fetch posts!
        if (memory_posts["hot"] == null || memory_posts["recent"] == null || memory_posts["top"] == null) {
            Log.d("Debug", "Null post values in PostsMain! " + memory_posts["hot"] + " | " + memory_posts["recent"] + " | " + memory_posts["top"])
            latest_errmsg = "Null post values in PostsMain! " + memory_posts["hot"] + " | " + memory_posts["recent"] + " | " + memory_posts["top"]
            startActivity(Intent(this, ErrorDisplay::class.java))
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