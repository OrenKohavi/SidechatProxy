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
import com.example.sidechatproxy.API_Handler.Companion.getImageFromUrl
import com.example.sidechatproxy.StartupScreen.Companion.token
import java.text.SimpleDateFormat
import java.util.*


class AdapterPost(
    private val context:Context,
    private var postList:List<Post>,
    val category: String
) : RecyclerView.Adapter<AdapterPost.HolderPost>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPost {
        val view = LayoutInflater.from(context).inflate(R.layout.single_post, parent, false)
        return HolderPost(view)
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

        //Parse Date
        var dateString: String
        try {
            //Time stuff is fucky, so just put it all in a try/catch
            val dateFormat = SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("GMT");
            //Log.d("Debug", "Current timezone is: " + TimeZone.getDefault())
            val postedTime = dateFormat.parse(created_at)!!
            val time = postedTime.time
            val now = System.currentTimeMillis()
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
        this.notifyDataSetChanged()
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