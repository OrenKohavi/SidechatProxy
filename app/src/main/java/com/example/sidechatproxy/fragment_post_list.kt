package com.example.sidechatproxy

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val CATEGORY = "hot_posts"

/**
 * A simple [Fragment] subclass.
 * Use the [fragment_post_list.newInstance] factory method to
 * create an instance of this fragment.
 */
class fragment_post_list : Fragment() {
    private var category: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = it.getString(CATEGORY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_post_list, container, false)
        //val textView = view.findViewById<TextView>(R.id.placeholder)
        //@Suppress("UNCHECKED_CAST")
        //textView.text = (info_in_memory[category + "_posts"] as List<Post>)[0].body
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(category: String) =
            fragment_post_list().apply {
                arguments = Bundle().apply {
                    putString(CATEGORY, category)
                }
            }
    }
}