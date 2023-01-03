package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
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

        ageField.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                click_continue(ageField.text.toString())
                handled = true
            }
            handled
        }

        continueButton.setOnClickListener {
            click_continue(ageField.text.toString())
        }
    }

    private fun click_continue(age: String) {
        Log.d("Debug", "Age Continue Clicked")
        val age_number = age.toInt()
        if (age_number < 13) {
            show_dialog(this, "Too Young", "You are too young to use SideChat!")
            //TODO: Perhaps add a mechanism where the user can't simply try again with a higher age
            // ^ Currently this age check is basically a formality and doesn't have any teeth
            return
        }
        if (age_number > 120) {
            show_dialog(this, "Too Old", "You are either from the future where life expectancy is much higher, or you're lying. $age_number years old, really?")
            return
        }
        if (age_number == 69) { //I'm really funny but nobody will ever discover this
            Toast.makeText(this, "Nice", Toast.LENGTH_LONG).show()
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