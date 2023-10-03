package com.orenkohavi.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.orenkohavi.sidechatproxy.API_Handler.Companion.check_email_verification


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
                    LoadingScreen.setup_loading_screen(
                        PostsMain::class.java,
                        StartupScreen.load_everything_runnable
                    )
                    startActivity(Intent(app_context, LoadingScreen::class.java))
                } else {
                    //try again in one second
                    mainHandler.postDelayed(this, 1000)
                }
            }
        })
    }
}