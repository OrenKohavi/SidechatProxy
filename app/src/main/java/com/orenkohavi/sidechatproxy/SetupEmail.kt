package com.orenkohavi.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.orenkohavi.sidechatproxy.StartupScreen.Companion.show_dialog


class SetupEmail : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_email)
        Log.d("Debug", "SetupEmail created")
        val continueButton: Button = findViewById(R.id.email_continue_button)
        val emailField: EditText = findViewById(R.id.email_field)

        emailField.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                click_continue(emailField.text.toString())
                handled = true
            }
            handled
        }

        continueButton.setOnClickListener {
            click_continue(emailField.text.toString())
        }
    }

    private fun click_continue(email: String) {
        Log.d("Debug", "Email Continue Clicked")
        if (!isValidEmail(email)){
            show_dialog(this, "Invalid Email", "The email '$email' is not valid")
            return
        }
        try {
            Log.d("Debug", "Calling register_email")
            val message: String = API_Handler.register_email(email)
            if (message.isNotEmpty()) {
                show_dialog(this, "Message", message)
                return
            }
            val switchActivityIntent = Intent(this, SetupWaitForEmailVerification::class.java)
            startActivity(switchActivityIntent)
        } catch (e : APIException) {
            StartupScreen.latest_errmsg = e.message.toString()
            Log.d("Debug", "Error Message from register_email ${e.message.toString()}")
            val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
            startActivity(switchActivityIntent)
        }
    }

    fun isValidEmail(target: String): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }
}