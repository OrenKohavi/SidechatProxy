package com.example.sidechatproxy

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetupTwoFactor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_twofactor)
        val continueButton: Button = findViewById(R.id.twofactor_continue_button)
        val phoneNumberField: EditText = findViewById(R.id.twofactor_field)
        continueButton.setOnClickListener {

        }
    }
}