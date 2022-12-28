package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.API_Handler.Companion.check_email_verification


class SetupWaitForEmailVerification : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_email_verification)
        val app_context = this
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                //Check if email is verified
                val email_verified = check_email_verification()
                if (email_verified) {
                    val switchActivityIntent = Intent(app_context, PostsMain::class.java)
                    startActivity(switchActivityIntent)
                } else {
                    //try again in one second
                    mainHandler.postDelayed(this, 1000)
                }
            }
        })
    }
}