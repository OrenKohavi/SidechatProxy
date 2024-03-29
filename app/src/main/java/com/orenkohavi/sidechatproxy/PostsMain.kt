package com.orenkohavi.sidechatproxy

import android.content.Intent
import android.graphics.Color.parseColor
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.orenkohavi.sidechatproxy.PostsStateAdapter.Companion.page_to_category
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.group_id
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.latest_errmsg
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.memory_posts
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.memory_strings
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.token
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.user_id
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class PostsStateAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    companion object {
        val page_to_category: Map<Int, PostType> = mapOf(0 to PostType.Hot, 1 to PostType.New, 2 to PostType.Top)
    }

    override fun getItemCount() = 3 //Hardcoded 3 pages (Hot, New, Top)

    override fun createFragment(position: Int): Fragment {
        val category: PostType = page_to_category[position] ?: throw IllegalStateException("Page position was $position -- Illegal!")
        return fragment_post_list.newInstance(category)
    }
}

class PostsMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_posts_main)
        val window: Window = this.window
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        memory_strings["cursor"] = ""
        if (memory_strings["group_color"]?.length != 7) {
            //Basic sanity check for the right format: #XXXXXX
            //If not, just set it to grey or something
            Log.d(
                "Debug",
                "Bad color format! --> ${memory_strings["group_color"]} <-- setting to #CCCCCC"
            )
            memory_strings["group_color"] = "#CCCCCC" // @color/light_grey
        }
        Log.d("Debug", "Setting statusBar to: ${memory_strings["group_color"]}")
        window.statusBarColor = parseColor(memory_strings["group_color"])
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

        val adapter = PostsStateAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(
            tabLayout, viewPager
        ) { tab, position ->
            tab.text = page_to_category.getOrDefault(position, "Unknown").toString().replaceFirstChar { it.uppercaseChar() }
        }.attach()

    }
}