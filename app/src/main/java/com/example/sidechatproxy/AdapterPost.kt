package com.example.sidechatproxy

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.text.SimpleDateFormat
import java.util.*

class AdapterPost(
    private val context:Context,
    private val postList:List<Post>,

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

        //2023-01-01T20:00:07.448Z

        //val dateFormat = SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss:SSSZ", Locale.US)
        //val dateFormat2 = SimpleDateFormat("MMM DD", Locale.US)
        //val postedTime = dateFormat.parse(created_at)!!
        //val formattedDate = dateFormat2.format(postedTime)

        holder.post_body.text = body
        holder.post_info.text = created_at
        holder.vote_counter.text = num_upvotes.toString()
        holder.post_image.visibility = GONE
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    inner class HolderPost(itemView: View) : ViewHolder(itemView) {
        var upvote_button: ImageButton = itemView.findViewById(R.id.upvote_button)
        var downvote_button: ImageButton = itemView.findViewById(R.id.downvote_button)
        var comment_button: ImageButton = itemView.findViewById(R.id.comment_button)
        var vote_counter: TextView = itemView.findViewById(R.id.vote_counter)
        var post_body: TextView = itemView.findViewById(R.id.post_body)
        var post_info: TextView = itemView.findViewById(R.id.post_info)
        var post_image: ImageView = itemView.findViewById(R.id.post_image)
    }
}