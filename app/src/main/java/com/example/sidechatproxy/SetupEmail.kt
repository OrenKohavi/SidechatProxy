package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetupEmail : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_email)
        val continueButton: Button = findViewById(R.id.email_continue_button)
        val emailField: EditText = findViewById(R.id.email_field)
        continueButton.setOnClickListener {
            Log.d("Debug", "Email Continue Clicked")
            val email: String = emailField.text.toString()
            try {
                API_Handler.register_email(email)
                val switchActivityIntent = Intent(this, SetupWaitForEmailVerification::class.java)
                startActivity(switchActivityIntent)
            } catch (e : APIException) {
                StartupScreen.latest_errmsg = e.message.toString()
                val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
                startActivity(switchActivityIntent)
            }
        }
    }
}