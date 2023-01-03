package com.example.sidechatproxy

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sidechatproxy.API_Handler.Companion.get_all_posts
import com.example.sidechatproxy.API_Handler.Companion.get_posts
import com.example.sidechatproxy.StartupScreen.Companion.group_id
import com.example.sidechatproxy.StartupScreen.Companion.memory_posts
import com.example.sidechatproxy.StartupScreen.Companion.token

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val CATEGORY = "hot_posts"

/**
 * A simple [Fragment] subclass.
 * Use the [fragment_post_list.newInstance] factory method to
 * create an instance of this fragment.
 */
class fragment_post_list : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var category: String? = null
    private lateinit var adapterPost: AdapterPost
    private lateinit var swr: SwipeRefreshLayout
    companion object {
        @JvmStatic
        fun newInstance(category: String) =
            fragment_post_list().apply {
                arguments = Bundle().apply {
                    putString(CATEGORY, category)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = it.getString(CATEGORY)
        }
        Log.d("Debug", "Creating post list with category: $category")
        //Make sure it has the right posts
        if (memory_posts[category] == null) {
            Log.d("Debug","Category $category was null in onCreate!")
            get_all_posts() //Lazy fix
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_post_list, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.post_list_recyclerview)
        adapterPost = AdapterPost(requireContext(), memory_posts[category]!!, category!!)
        recyclerView.adapter = adapterPost
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.setBackgroundColor(Color.parseColor(StartupScreen.memory_strings["group_color"]))

        swr = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        swr.setOnRefreshListener(this)
        //val textView = view.findViewById<TextView>(R.id.placeholder)
        //@Suppress("UNCHECKED_CAST")
        //textView.text = (info_in_memory[category + "_posts"] as List<Post>)[0].body
        return view
    }

    override fun onRefresh() {
        Log.d("Debug", "Refreshing Posts w/ swipeup")
        val post_getter_future = API_Handler.get_returnfuture(
            "https://api.sidechat.lol/v1/posts?group_id=$group_id&type=${adapterPost.category}",
            token
        )
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.postDelayed(object : Runnable {
            override fun run() {
                val new_post_list = get_posts(post_getter_future)
                Log.d("Debug", "Done Refreshing")
                memory_posts[adapterPost.category] = new_post_list
                adapterPost.updatePostList(memory_posts[adapterPost.category]!!)
                swr.isRefreshing = false
            }
        }, 200)
    }
}