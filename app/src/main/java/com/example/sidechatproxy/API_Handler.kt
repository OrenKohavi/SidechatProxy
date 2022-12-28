package com.example.sidechatproxy

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.example.sidechatproxy.MainActivityDecider.Companion.info_in_memory
import com.example.sidechatproxy.MainActivityDecider.Companion.longterm_put
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import android.provider.Settings
import com.example.sidechatproxy.MainActivityDecider.Companion.longterm_get
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask


class API_Handler {
    companion object {
        private fun parse_user(user: Map<String, Any>) {
            info_in_memory["user_stored"] = true
            info_in_memory["user_id"] = user["id"] as String
        }

        private fun parse_group(group: Map<String, Any>) {
            info_in_memory["group_stored"] = true
            info_in_memory["group_color"] = group["color"] as String
            info_in_memory["group_name"] = group["name"] as String
        }

        private fun parse_posts(hot: Map<String, Any>, new: Map<String, Any>, top: Map<String, Any>) {
            info_in_memory["posts_stored"] = true
            info_in_memory["posts_hot"] = hot
            info_in_memory["posts_new"] = new
            info_in_memory["posts_top"] = top
        }

        fun get_posts(group_id: String, type: String): Array<Map<String, String>> {
            return emptyArray()
        }

        fun check_email_verification(email: String): Boolean {
            val token: String = info_in_memory["token"] as String
            val get_response = get("https://api.sidechat.lol/v1/users/check_email_verified", token)
            if (get_response.isEmpty()) {
                return false
            } else {
                val response_json = get_response["verified_email_updates_response"]
                if (response_json !is Map<*, *>) {
                    Log.d("Debug_API", "response_json exists but is not a map!")
                    Log.d("Debug_API", "Content of response_json is: $response_json")
                    throw APIException("response_json incorrect format: $response_json")
                }
                val user: Map<String, Any>
                val group: Map<String, Any>
                try {
                    user = response_json["user"] as Map<String, Any>
                    group = response_json["group"] as Map<String, Any>
                } catch (e: java.lang.Exception) {
                    Log.d("Debug", "Error converting to Map: $response_json")
                    throw APIException("Error converting to map: $response_json")
                }
                parse_user(user)
                parse_group(group)
                return true
            }
        }

        fun register_email(email: String) {
            val token: String = info_in_memory["token"] as String
            val get_response = get("https://api.sidechat.lol/v1/login_type?email=$email", token)
            if (get_response.isNotEmpty()) {
                throw APIException("email get falied: $get_response")
            }
            val post_response = post(
                "https://api.sidechat.lol/v2/users/register_email",
                mapOf("email" to email),
                token
            )
            if (post_response.isNotEmpty()) {
                throw APIException("email post falied: $post_response")
            }
        }

        fun complete_registration(ctx: Activity, age: String) {
            val age_number = age.toInt()
            val url = "https://api.sidechat.lol/v1/complete_registration"
            val data = mapOf(
                "registration_id" to info_in_memory["registration_id"].toString(),
                "age" to age_number,
            )
            val response = post(url, data)
            Log.d("Debug_API", "Registration Completion Response $response")
            val token = response.getOrDefault("token", false)
            val user = response.getOrDefault("user", false)
            Log.d("Debug_API", "Token is: $token")
            Log.d("Debug_API", "User is: $user")
            if (token == false) {
                throw APIException(response.toString())
            } else {
                info_in_memory["token"] = token as String
                longterm_put(ctx, "token", token)
                //Need to register device token
                val device_id = getDeviceID(ctx)
                info_in_memory["device_id"] = device_id
                //Need to use the token as a bearer token
                val device_token_url = "https://api.sidechat.lol/v1/register_device_token"
                val device_token_data = mapOf(
                    "build_type" to "release",
                    "bundle_id" to "com.flowerave.sidechat",
                    "device_token" to device_id,
                )
                val device_token_response = post(device_token_url, device_token_data, info_in_memory["token"].toString())
                Log.d("Debug_API", "device_token_response was: $device_token_response")
                if (device_token_response.isNotEmpty()) {
                    throw APIException(device_token_response.toString())
                }
            }
        }

        fun phone_verify(ctx: Activity, twofactor_code: String): Boolean {
            val twofactor_code_upper = twofactor_code.uppercase()
            val url = "https://api.sidechat.lol/v1/verify_phone_number"
            val data = mapOf("code" to twofactor_code_upper, "phone_number" to info_in_memory["phone_number"].toString())
            val response = post(url, data)
            Log.d("Debug_API", "Phone Number Verification Response $response")
            //If this is a new user, the response will include a registration_id and setup must continue
            //Otherwise, it includes a user object and a token
            val logged_in_user = response.getOrDefault("logged_in_user", false)
            if (logged_in_user == false) {
                val registration_id = response.getOrDefault("registration_id", false)
                if (registration_id == false) {
                    throw APIException(response.toString())
                } else {
                    info_in_memory["registration_id"] = registration_id as String
                }
                return true //Additional setup required, go to SetupAge
            } else {
                if (logged_in_user !is Map<*, *>) {
                    Log.d("Debug_API", "Logged_in_user exists but is not a map!")
                    Log.d("Debug_API", "Content of logged_in_user is: $logged_in_user")
                    throw APIException("Logged in user incorrect format: $logged_in_user")
                }
                val user: Map<String, Any> = logged_in_user["user"] as Map<String, Any>
                val group: Map<String, Any> = logged_in_user["group"] as Map<String, Any>
                val token: String = logged_in_user["token"] as String
                Log.d("Debug_API", "Values retrieved! Token is: $token, group is $group, user is $user")
                parse_user(user)
                parse_group(group)
                longterm_put(ctx, "token", token)
                return false //No additional setup required
            }
        }

        fun login_register(phoneNumber: String) {
            //Send the API call to sidechat
            val phoneNumber_with_country_code = "+1$phoneNumber"
            val url = "https://api.sidechat.lol/v1/login_register"
            val data = mapOf("version" to 3, "phone_number" to phoneNumber_with_country_code)
            val response = post(url, data)
            if (response.isNotEmpty()) {
                Log.d("Debug", "Response was not empty! Throwing error")
                throw APIException(response.toString())
            } else {
                Log.d("Debug", "Login Register Successful")
                //Save the phone number for the next steps
                info_in_memory["phone_number"] = phoneNumber_with_country_code
            }
        }

        fun get(plaintext_url: String, bearer_token: String?): Map<String, Any> {
            val client = OkHttpClient()
            val url = URL(plaintext_url)

            val request = if (bearer_token != null) {
                Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer $bearer_token")
                    .build()
            } else {
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            }

            val response = client.newCall(request).execute()

            val responseBody = response.body!!.string()

            //Response
            Log.d("Debug_API", "Response Body: $responseBody")

            val mapper = jacksonObjectMapper()
            val objData: Map<String, Any> = mapper.readValue(responseBody, Map::class.java) as Map<String, Any>

            Log.d("Debug_API", "Parsed Response is: $objData")

            return objData
        }

        //Version of the post method with no token
        fun post(url: String, args: Map<String, Any>): Map<String, Any> {
            return post(url, args, null)
        }

        fun post(url: String, args: Map<String, Any>, bearer_token: String?): Map<String, Any> {
            val post_callable = Callable {
                return@Callable _post(url, args, bearer_token)
            }
            val future_callable = FutureTask(post_callable)
            val thread = Thread(future_callable)
            thread.start()
            //TODO: Figure out how to make this API call non-blocking
            // ^ Ugh I might need to learn how to use volley
            val result = future_callable.get() //Blocks until the request is done :(
            return result
        }

        fun _post(plaintext_url : String, args : Map<String, Any>, bearer_token: String?): Map<String, Any> {
            val client = OkHttpClient()
            val url = URL(plaintext_url)

            //convert args to a json string:
            val jsonString: String
            try {
                val jsonMakerMapper = jacksonObjectMapper()
                jsonString = jsonMakerMapper.writeValueAsString(args)
                Log.d("Debug_API", "Created this JSON to send: $jsonString")
            } catch (e: JsonProcessingException) {
                e.printStackTrace()
                throw IllegalArgumentException("Could not convert JSON to string")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonString.toRequestBody(mediaType)

            val request = if (bearer_token != null) {
                Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer $bearer_token")
                    .build()
            } else {
                Request.Builder()
                    .url(url)
                    .post(body)
                    .build()
            }

            val response = client.newCall(request).execute()

            val responseBody = response.body!!.string()

            //Response
            Log.d("Debug_API", "Response Body: $responseBody")

            //use jackson to turn the json into a map
            val mapper = jacksonObjectMapper()
            val objData: Map<String, Any> = mapper.readValue(responseBody, Map::class.java) as Map<String, Any>

            Log.d("Debug_API", "Parsed Response is: $objData")

            return objData
        }

        @SuppressLint("HardwareIds") //Potentially not recommended to use ANDROID_ID here, but whatever
        fun getDeviceID(ctx: Activity): String {
            val existing_id = longterm_get(ctx, "device_id")
            if (existing_id != null) {
                //We already have an ID!
                return existing_id
            }
            //Sidechat wants device IDs to be 64 hexadecimal characters (256bits)
            // Android IDs are 64 bits
            // So I'll just use a SHA256 hash to extend to 256 bits
            fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
            val androidID = Settings.Secure.getString(ctx.contentResolver,Settings.Secure.ANDROID_ID)
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(androidID.toByteArray())
            val hash_string = hash.toHexString()
            longterm_put(ctx, "device_id", hash_string)
            return hash_string
        }
    }
}