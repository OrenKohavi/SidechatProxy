package com.orenkohavi.sidechatproxy

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.orenkohavi.sidechatproxy.R
import com.orenkohavi.sidechatproxy.API_Handler.Companion.get_all_post_categories
import com.orenkohavi.sidechatproxy.API_Handler.Companion.parse_posts_from_future
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.group_id
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.memory_posts
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.token

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val CATEGORY = "default_category_should_not_be_used"

/**
 * A simple [Fragment] subclass.
 * Use the [fragment_post_list.newInstance] factory method to
 * create an instance of this fragment.
 */
class fragment_post_list : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var category: PostType? = null
    private lateinit var adapterPost: AdapterPost
    private lateinit var swr: SwipeRefreshLayout
    companion object {
        @JvmStatic
        fun newInstance(category: PostType) =
            fragment_post_list().apply {
                arguments = Bundle().apply {
                    putString(CATEGORY, category.as_string())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = it.getString(CATEGORY)!!.getPostType()
        }
        Log.d("Debug", "Creating post list with category: $category")
        //Make sure it has the right posts
        if (memory_posts[category!!.as_string()] == null) {
            Log.d("Debug","Category ${category!!.as_string()} was null in onCreate!")
            get_all_post_categories() //Lazy fix
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
        Log.d("Debug", "Creating AdapterPost with category: $category (as string, ${category!!.as_string()})")
        adapterPost = AdapterPost(requireContext(), memory_posts[category!!.as_string()]!!, category!!)
        recyclerView.adapter = adapterPost
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.setBackgroundColor(Color.parseColor(StartupScreen.memory_strings["group_color"]))
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (adapterPost.loading_disabled || adapterPost.needs_loading) {
                    return
                }
                val layoutManager = LinearLayoutManager::class.java.cast(recyclerView.layoutManager)
                if (layoutManager == null) {
                    Log.d("Debug", "layoutManager was null in onScrolled")
                    return
                }
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                val endHasBeenReached = lastVisible + 5 >= totalItemCount
                if (endHasBeenReached) {
                    Log.d("Debug", "End has been reached")
                    adapterPost.needs_loading = true
                    adapterPost.loadMorePosts()
                    Log.d("Debug", "finished loading more posts")
                }
            }
        })

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
        post_getter_future.run()
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.postDelayed(object : Runnable {
            @SuppressLint("NotifyDataSetChanged")
            override fun run() {
                parse_posts_from_future(post_getter_future, adapterPost.category)
                Log.d("Debug", "Done Refreshing")
                adapterPost.updatePostList(memory_posts[adapterPost.category.toString()]!!)
                swr.isRefreshing = false
                adapterPost.notifyDataSetChanged()
            }
        }, 100)
    }
}