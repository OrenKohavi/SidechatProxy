package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg
import com.example.sidechatproxy.StartupScreen.Companion.load_everything_runnable
import com.example.sidechatproxy.StartupScreen.Companion.show_dialog
import com.example.sidechatproxy.StartupScreen.Companion.token


class SetupTwoFactor : AppCompatActivity() {
    companion object {
        enum class TwoFactorResponse {
            Auth_Complete,
            Auth_Required,
            Auth_Failed,
            Auth_Email_Required
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_twofactor)
        val continueButton: Button = findViewById(R.id.twofactor_continue_button)
        val twofactorNumberField: EditText = findViewById(R.id.twofactor_field)

        twofactorNumberField.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                click_continue(twofactorNumberField.text.toString())
                handled = true
            }
            handled
        }

        continueButton.setOnClickListener {
            click_continue(twofactorNumberField.text.toString())
        }
    }

    private fun click_continue(twofactor_code: String) {
        Log.d("Debug", "Twofactor Continue Clicked")
        if (twofactor_code.length != 6) {
            show_dialog(this, "Bad Format", "Codes should be 6 characters long")
            return
        }
        try {
            val response: TwoFactorResponse = API_Handler.phone_verify(twofactor_code)
            if (response == TwoFactorResponse.Auth_Failed) {
                show_dialog(this, "Verification Failed", latest_errmsg)
                return
            }
            if (response == TwoFactorResponse.Auth_Email_Required) {
                Log.d("Debug", "Going to SetupEmail")
                startActivity(Intent(this, SetupEmail::class.java))
                return
            }
            val needs_additional_setup = (response == TwoFactorResponse.Auth_Required)
            if (needs_additional_setup) {
                val switchActivityIntent = Intent(this, SetupAge::class.java)
                startActivity(switchActivityIntent)
            } else {
                //Go straight to PostsMain
                LoadingScreen.setup_loading_screen(
                    PostsMain::class.java,
                    load_everything_runnable
                ) //Setup loading screen
                startActivity(Intent(this, LoadingScreen::class.java)) //Go to loading screen
            }
        } catch (e : APIException) {
            StartupScreen.latest_errmsg = e.message.toString()
            val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
            startActivity(switchActivityIntent)
        }
    }
}