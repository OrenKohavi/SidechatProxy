package com.example.sidechatproxy

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.text.SimpleDateFormat
import java.util.*


class AdapterComments(
    private val context:Context,
    private var commentList:List<Comment>,
) : RecyclerView.Adapter<AdapterComments.HolderComment>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderComment {
        val view = LayoutInflater.from(context).inflate(R.layout.single_post, parent, false)
        return HolderComment(view)
    }

    override fun onBindViewHolder(holder: HolderComment, position: Int) {
        val model = commentList[position]
        val body: String = model.body
        val num_upvotes: Number = model.num_upvotes
        val created_at: String = model.created_at

        holder.post_body.text = body
        if (body.isEmpty()) {
            holder.post_body.visibility = View.GONE
        }
        holder.vote_counter.text = num_upvotes.toString()

        holder.image_cardview.invalidate()
        holder.image_cardview.requestLayout()

        //Parse Date
        var dateString: String
        try {
            Log.d("Debug", "Time of Post: $created_at")
            //Time stuff is fucky, so just put it all in a try/catch
            val dateFormat = SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
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
    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    inner class HolderComment(itemView: View) : ViewHolder(itemView) {
        var vote_counter: TextView = itemView.findViewById(R.id.vote_counter)
        var post_body: TextView = itemView.findViewById(R.id.post_body)
        var post_info: TextView = itemView.findViewById(R.id.post_info)
        var image_cardview: CardView = itemView.findViewById(R.id.image_cardview)
    }
}