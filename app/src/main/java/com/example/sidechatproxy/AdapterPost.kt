package com.example.sidechatproxy

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.sidechatproxy.API_Handler.Companion.background_fetch_post_images
import com.example.sidechatproxy.API_Handler.Companion.getImageFromUrl
import com.example.sidechatproxy.API_Handler.Companion.get_posts_with_cursor
import com.example.sidechatproxy.StartupScreen.Companion.memory_posts
import com.example.sidechatproxy.StartupScreen.Companion.memory_strings
import com.example.sidechatproxy.StartupScreen.Companion.token
import java.text.SimpleDateFormat
import java.util.*


class AdapterPost(
    private val context:Context,
    private var postList:List<Post>,
    val category: PostType
) : RecyclerView.Adapter<AdapterPost.HolderPost>() {
    var loading_disabled: Boolean = category != PostType.Hot
    var needs_loading: Boolean = false
    var mRecyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPost {
        val view = LayoutInflater.from(context).inflate(R.layout.single_post, parent, false)
        return HolderPost(view)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onBindViewHolder(holder: HolderPost, position: Int) {
        val model = postList[position]
        val body: String = model.body
        val num_upvotes: Number = model.num_upvotes
        val image_url: String? = model.image_url
        val num_comments: Number = model.num_comments
        val created_at: String = model.created_at

        holder.post_body.text = body
        if (body.isEmpty()) {
            holder.post_body.visibility = View.GONE
        }
        holder.vote_counter.text = num_upvotes.toString()
        holder.comment_counter.text = num_comments.toString()

        bind_buttons(holder, context)

        holder.image_cardview.invalidate()
        holder.image_cardview.requestLayout()

        //Parse Date
        var dateString: String
        try {
            //Time stuff is fucky, so just put it all in a try/catch
            //Log.d("Debug", "Original Time is: $created_at")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            //Log.d("Debug", "Current timezone is: " + TimeZone.getDefault())
            val postedTime = dateFormat.parse(created_at)!!
            //Log.d("Debug", "Posted Time is: $postedTime")
            val time = postedTime.time
            val now = System.currentTimeMillis()
            //Log.d("Debug", "Now Time is: $now")
            val ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS, 0x00080000) //Flag is FORMAT_ABBREV_ALL
            dateString = ago as String
        } catch (e : Exception) {
            Log.d("Debug", "Time parsing exception! ${e.printStackTrace()}")
            dateString = "Unknown Time Ago"
        }
        holder.post_info.text = dateString

        //Load and handle image
        if (image_url != null) {
            holder.post_image.visibility = View.VISIBLE
            holder.image_cardview.visibility = View.VISIBLE
            //Log.d("Debug", "Token: $token")
            Log.d("Debug", "Getting image from url: $image_url")
            getImageFromUrl(image_url, holder.post_image, token!!)
        } else {
            holder.post_image.setImageBitmap(null)
            holder.image_cardview.visibility = View.GONE
            holder.post_image.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    fun updatePostList(new_list: List<Post>) {
        postList = new_list
    }

    fun loadMorePosts() {
        val cursor: String? = memory_strings["cursor_${category.as_string()}"]
        if (cursor != null) {
            Log.d("Debug", "Loading more posts with cursor: $cursor")
        } else {
            Log.d("Debug", "Cursor is Blank!")
            Toast.makeText(context, "Error fetching additional posts", Toast.LENGTH_SHORT).show()
            return
        }
        val old_dataset_size = this.itemCount
        get_posts_with_cursor(cursor, category)
        this.postList = memory_posts[category.as_string()]!!
        Log.d("Debug", "Old dataset size: $old_dataset_size")
        Log.d("Debug", "New dataset size: ${this.itemCount}")
        mRecyclerView?.post {
            this.notifyItemRangeInserted(old_dataset_size, this.itemCount)
            background_fetch_post_images(category)
            this.needs_loading = false
        }
    }

    private fun bind_buttons(holder: HolderPost, ctx: Context){
        holder.upvote_button.setOnClickListener {
            Toast.makeText(ctx, "Upvote Not Supported Yet", Toast.LENGTH_SHORT).show()
        }
        holder.downvote_button.setOnClickListener {
            Toast.makeText(ctx, "Downvote Not Supported Yet", Toast.LENGTH_SHORT).show()
        }
        holder.comment_button.setOnClickListener {
            Toast.makeText(ctx, "Comments Not Supported Yet", Toast.LENGTH_SHORT).show()
        }
        //holder.dm_button.setOnClickListener {
        //    Toast.makeText(ctx, "DMs Not Supported Yet", Toast.LENGTH_SHORT).show()
        //}
    }

    inner class HolderPost(itemView: View) : ViewHolder(itemView) {
        var upvote_button: ImageButton = itemView.findViewById(R.id.upvote_button)
        var downvote_button: ImageButton = itemView.findViewById(R.id.downvote_button)
        var comment_button: ImageButton = itemView.findViewById(R.id.comment_button)
        var vote_counter: TextView = itemView.findViewById(R.id.vote_counter)
        var post_body: TextView = itemView.findViewById(R.id.post_body)
        var post_info: TextView = itemView.findViewById(R.id.post_info)
        var image_cardview: CardView = itemView.findViewById(R.id.image_cardview)
        var post_image: ImageView = itemView.findViewById(R.id.post_image)
        var comment_counter: TextView = itemView.findViewById(R.id.comment_counter)
        //var dm_button: ImageButton = itemView.findViewById(R.id.dm_button)
    }
}