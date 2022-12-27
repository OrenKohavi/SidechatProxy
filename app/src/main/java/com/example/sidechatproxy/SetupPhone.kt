package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class SetupPhone :  AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Debug", "SetupPhone Instance Created")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_phone)
        val continueButton: Button = findViewById(R.id.phonenumber_continue_button)
        val phoneNumberField: EditText = findViewById(R.id.phone_field)
        continueButton.setOnClickListener {
            Log.d("Debug", "Phone Continue Clicked")
            //Check if the phone number is valid
            val phoneNumber: String = phoneNumberField.text.toString()
            if (phoneNumber == "75477547") {
                val switchActivityIntent = Intent(this, SetupTwoFactor::class.java)
                startActivity(switchActivityIntent)
            }
            if (checkValidPhone(phoneNumber)) {
                try {
                    API_Handler.login_register(phoneNumber)
                } catch (e : APIException) {
                    MainActivityDecider.latest_errmsg = e.message.toString()
                    val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
                    startActivity(switchActivityIntent)
                }

            } else {
                Toast.makeText(applicationContext, "Phone Number Invalid!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkValidPhone(phoneNumber: String): Boolean {
        return phoneNumber.length > 8;
    }
}