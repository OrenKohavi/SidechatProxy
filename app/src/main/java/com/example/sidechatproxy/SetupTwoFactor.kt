package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.MainActivityDecider.Companion.debug_mode

class SetupTwoFactor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_twofactor)
        val continueButton: Button = findViewById(R.id.twofactor_continue_button)
        val twofactorNumberField: EditText = findViewById(R.id.twofactor_field)
        continueButton.setOnClickListener {
            Log.d("Debug", "Twofactor Continue Clicked")
            val twofactor_code: String = twofactorNumberField.text.toString()
            try {
                val needs_additional_setup = API_Handler.phone_verify(this, twofactor_code)
                if (needs_additional_setup) {
                    val switchActivityIntent = Intent(this, SetupAge::class.java)
                    startActivity(switchActivityIntent)
                } else {
                    val switchActivityIntent = Intent(this, PostsMain::class.java)
                    startActivity(switchActivityIntent)
                }
            } catch (e : APIException) {
                MainActivityDecider.latest_errmsg = e.message.toString()
                val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
                startActivity(switchActivityIntent)
            }
        }
    }
}