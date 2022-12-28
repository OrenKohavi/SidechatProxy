package com.example.sidechatproxy

import android.annotation.SuppressLint
import android.util.Log
import com.example.sidechatproxy.StartupScreen.Companion.info_in_memory
import com.example.sidechatproxy.StartupScreen.Companion.longterm_put
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import android.provider.Settings
import com.example.sidechatproxy.StartupScreen.Companion.longterm_get
import com.example.sidechatproxy.StartupScreen.Companion.startup_activity_context
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
            info_in_memory["group_id"] = group["id"] as String
            info_in_memory["group_icon_url"] = group["icon_url"] as String
            longterm_put("group_id", group["id"] as String)
        }

        fun get_user_and_group() {
            Log.d("Debug_API", "updating user and group")
            val group_id = longterm_get("group_id") ?: throw APIException("Stored group_id was null!")
            val token: String = longterm_get("token") ?: throw APIException("Stored token was null!")
            info_in_memory["token"] = token //For future API calls, so that they don't need to use longterm memory
            Log.d("Debug_API", "Set token in memory to: $token")
            val response = get(
                "https://api.sidechat.lol/v1/updates?group_id=$group_id",
                token
            )
            val group: Map<String, Any>
            val user: Map<String, Any>
            try {
                @Suppress("UNCHECKED_CAST")
                group = response["group"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                user = response["user"] as Map<String, Any>
            } catch (e : Exception) {
                throw APIException("Could not parse group/user in update_func\n$response")
            }
            parse_group(group)
            parse_user(user)
        }


        fun get_all_posts() {
            val group_id = info_in_memory["group_id"] as String
            //Still blocks, but at least now it's 3 paralell API calls rather than serially one at a time
            runBlocking {
                launch {
                    val hot_posts = get_posts(group_id, "hot")
                    info_in_memory["hot_posts"] = hot_posts
                }
                launch {
                    val new_posts = get_posts(group_id, "recent") //Weird, but their API wants the 'recent' keyword
                    info_in_memory["new_posts"] = new_posts
                }
                launch {
                    val top_posts = get_posts(group_id, "top")
                    info_in_memory["top_posts"] = top_posts
                }
            }
        }

        fun get_posts(group_id: String, type: String): List<Map<String, Any>> {
            val url = "https://api.sidechat.lol/v1/posts?group_id=$group_id&type=$type"
            val token = info_in_memory["token"] as String
            val response = get(url, token)
            val post_list = (response["posts"] ?: throw APIException("Post List is Null! Response: $response ||| $info_in_memory"))
            if (post_list !is ArrayList<*>) {
                throw APIException("post_list is not an ArrayList! Response: $response")
            }
            var new_post_list: MutableList<Map<String, Any>> = mutableListOf()
            for (post in post_list) {
                if (post !is Map<*, *>) {
                    throw APIException("Post was not a map! Post contents: $post, response: $response")
                }
                @Suppress("UNCHECKED_CAST") //Suppressing is okay because JSON is guaranteed to have string keys
                new_post_list.add(post as Map<String, Any>)
            }
            return new_post_list
        }

        fun check_email_verification(): Boolean {
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
                    @Suppress("UNCHECKED_CAST")
                    user = response_json["user"] as Map<String, Any>
                    @Suppress("UNCHECKED_CAST")
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

        fun complete_registration(age: String) {
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
                longterm_put("token", token)
                //Need to register device token
                val device_id = getDeviceID()
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

        fun phone_verify(twofactor_code: String): Boolean {
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
                val user: Map<String, Any>
                val group: Map<String, Any>
                try {
                    @Suppress("UNCHECKED_CAST")
                    user = logged_in_user["user"] as Map<String, Any>
                    @Suppress("UNCHECKED_CAST")
                    group = logged_in_user["group"] as Map<String, Any>
                } catch (e: java.lang.Exception) {
                    Log.d("Debug", "logged_in_user - Error converting to Map: $logged_in_user")
                    throw APIException("logged_in_user - Error converting to map: $logged_in_user")
                }
                val token: String = logged_in_user["token"] as String
                Log.d("Debug_API", "Values retrieved! Token is: $token, group is $group, user is $user")
                parse_user(user)
                parse_group(group)
                longterm_put("token", token)
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

        fun get(url: String, bearer_token: String?): Map<String, Any> {
            val get_callable = Callable {
                return@Callable _get(url, bearer_token)
            }
            val future_callable = FutureTask(get_callable)
            val thread = Thread(future_callable)
            thread.start()
            //TODO: Figure out how to make this API call non-blocking (same issue as with post)
            val result = future_callable.get() //Blocks until the request is done :(
            return result
        }

        fun _get(plaintext_url: String, bearer_token: String?): Map<String, Any> {
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
            @Suppress("UNCHECKED_CAST") //Safe to assume string keys, because JSON can only have string keys
            val objData: Map<String, Any> = mapper.readValue(responseBody, Map::class.java) as Map<String, Any>

            Log.d("Debug_API", "Parsed Response is: $objData")

            return objData
        }

        //Version of the post method with no token
        private fun post(url: String, args: Map<String, Any>): Map<String, Any> {
            return post(url, args, null)
        }

        private fun post(url: String, args: Map<String, Any>, bearer_token: String?): Map<String, Any> {
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

        private fun _post(plaintext_url : String, args : Map<String, Any>, bearer_token: String?): Map<String, Any> {
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
            @Suppress("UNCHECKED_CAST") //Safe to assume string keys, because JSON can only have string keys
            val objData: Map<String, Any> = mapper.readValue(responseBody, Map::class.java) as Map<String, Any>

            Log.d("Debug_API", "Parsed Response is: $objData")

            return objData
        }

        @SuppressLint("HardwareIds") //Potentially not recommended to use ANDROID_ID here, but whatever
        fun getDeviceID(): String {
            val existing_id = longterm_get("device_id")
            if (existing_id != null) {
                //We already have an ID!
                return existing_id
            }
            //Sidechat wants device IDs to be 64 hexadecimal characters (256bits)
            // Android IDs are 64 bits
            // So I'll just use a SHA256 hash to extend to 256 bits
            fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
            val androidID = Settings.Secure.getString(startup_activity_context.contentResolver,Settings.Secure.ANDROID_ID)
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(androidID.toByteArray())
            val hash_string = hash.toHexString()
            longterm_put("device_id", hash_string)
            return hash_string
        }
    }
}