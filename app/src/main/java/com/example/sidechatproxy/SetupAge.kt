package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.StartupScreen.Companion.show_dialog

class SetupAge : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_age)
        val continueButton: Button = findViewById(R.id.age_continue_button)
        val ageField: EditText = findViewById(R.id.age_field)
        continueButton.setOnClickListener {
            Log.d("Debug", "Age Continue Clicked")
            val age = ageField.toString()
            val age_number = age.toInt()
            if (age_number < 13) {
                show_dialog(this, "Too Young", "You're too young to use SideChat!")
                return@setOnClickListener
            }
            try {
                API_Handler.complete_registration(age)
                val switchActivityIntent = Intent(this, SetupEmail::class.java)
                startActivity(switchActivityIntent)
            } catch (e : java.lang.NumberFormatException) {
                Toast.makeText(applicationContext, "Invalid Number", Toast.LENGTH_SHORT).show()
            } catch (e : APIException) {
                StartupScreen.latest_errmsg = e.message.toString()
                val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
                startActivity(switchActivityIntent)
            }
        }
    }
}