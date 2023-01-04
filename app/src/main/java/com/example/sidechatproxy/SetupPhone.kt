package com.example.sidechatproxy

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sidechatproxy.StartupScreen.Companion.show_dialog


class SetupPhone : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Debug", "SetupPhone Instance Created")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_phone)
        val continueButton: Button = findViewById(R.id.phonenumber_continue_button)
        val phoneNumberField: EditText = findViewById(R.id.phone_field)

        phoneNumberField.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                click_continue(phoneNumberField.text.toString())
                handled = true
            }
            handled
        }

        continueButton.setOnClickListener {
            click_continue(phoneNumberField.text.toString())
        }
    }

    private fun click_continue(pn: String) {
        Log.d("Debug", "Phone Continue Clicked")
        var phoneNumber = pn;
        phoneNumber = phoneNumber.removePrefix("+1") //Try to remove the +1 if it exists in the number
        //Check if the phone number is valid
        //I know that this is shitty (and also only supports the US, because that's the only API calls I managed to find)
        //Ideally, I would use PhoneNumberUtils.formatNumber or something
        if (phoneNumber.length < 10) {
            show_dialog(this, "Phone Number Too Short", "Please enter a 10-digit US phone number")
            return
        } else if (phoneNumber.length > 10) {
            show_dialog(this, "Phone Number Too Long", "Please enter a 10-digit US phone number")
            return
        }
        try {
            API_Handler.login_register(phoneNumber)
            val switchActivityIntent = Intent(this, SetupTwoFactor::class.java)
            startActivity(switchActivityIntent)
        } catch (e : APIException) {
            StartupScreen.latest_errmsg = e.message.toString()
            val switchActivityIntent = Intent(this, ErrorDisplay::class.java)
            startActivity(switchActivityIntent)
        }
    }

}