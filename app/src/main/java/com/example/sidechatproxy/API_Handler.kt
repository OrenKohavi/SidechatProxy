package com.example.sidechatproxy

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.util.Log
import com.example.sidechatproxy.StartupScreen.Companion.longterm_put
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import android.provider.Settings
import android.widget.ImageView
import com.example.sidechatproxy.StartupScreen.Companion.group_id
import com.example.sidechatproxy.StartupScreen.Companion.latest_errmsg
import com.example.sidechatproxy.StartupScreen.Companion.longterm_get
import com.example.sidechatproxy.StartupScreen.Companion.memory_posts
import com.example.sidechatproxy.StartupScreen.Companion.memory_strings
import com.example.sidechatproxy.StartupScreen.Companion.startup_activity_context
import com.example.sidechatproxy.StartupScreen.Companion.token
import com.example.sidechatproxy.StartupScreen.Companion.user_id
import okhttp3.*
import okio.IOException
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask


class API_Handler {
    companion object {
        private fun parse_user(user: Map<String, Any>) {
            user_id = user["id"] as String
            longterm_put("user_id", user_id!!)
            Log.d("Debug", "Longterm Stored user_id: $user_id")
        }

        private fun parse_group(group: Map<String, Any>) {
            group_id = group["id"] as String
            memory_strings["group_color"] = group["color"] as String
            memory_strings["group_name"] = group["name"] as String
            memory_strings["group_icon_url"] = group["icon_url"] as String
            longterm_put("group_id", group_id!!)
            longterm_put("group_color", group["color"] as String)
            Log.d("Debug", "Longterm Stored group_id: $group_id")
        }

        fun background_fetch_post_images(type: PostType) {
            val posts = memory_posts[type.as_string()]!!
            for (post in posts) {
                if (post.image_url != null) {
                    //TODO use a cache and prefetch the images that are not already in that cache
                }
            }
        }

        fun get_user_and_group() {
            Log.d("Debug_API", "updating user and group")
            val response_future = get_returnfuture(
                "https://api.sidechat.lol/v1/updates?group_id=$group_id",
                token
            )
            val group: Map<String, Any>
            val user: Map<String, Any>
            val response = response_future.get()
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

        fun get_posts_with_cursor(cursor: String, type: PostType) {
            Log.d("Debug", "Getting posts with cursor $cursor")
            val group_id = group_id!!
            val response_future = get_returnfuture(
                "https://api.sidechat.lol/v1/posts?cursor=$cursor&group_id=$group_id&type=${type.as_string()}",
                token
            )
            parse_posts_from_future(response_future, type)
        }

        fun get_all_post_categories() {
            Log.d("Debug", "Getting all posts")
            val group_id = group_id!!
            val token = (token ?: throw APIException("Token is null! Memory: $memory_strings | $memory_posts"))

            val hot_future = get_returnfuture("https://api.sidechat.lol/v1/posts?group_id=$group_id&type=hot", token)
            val new_future = get_returnfuture("https://api.sidechat.lol/v1/posts?group_id=$group_id&type=recent", token)
            val top_future = get_returnfuture("https://api.sidechat.lol/v1/posts?group_id=$group_id&type=top", token)
            //Hopefully all three GET requests are running simultaneously here (better than sequentially, but still not ideal)
            parse_posts_from_future(hot_future, PostType.Hot)
            parse_posts_from_future(new_future, PostType.New)
            parse_posts_from_future(top_future, PostType.Top)
            Log.d("Debug", "Finished getting all posts")
        }

        fun parse_posts_from_future(response_future: FutureTask<Map<String, Any>>, type: PostType) {
            val response = response_future.get()
            val post_list = (response["posts"] ?: throw APIException("Post List is Null! Response: $response ||| $memory_strings"))
            if (post_list !is ArrayList<*>) {
                throw APIException("post_list is not an ArrayList! Response: $response")
            }
            val new_post_list: MutableList<Post> = mutableListOf()
            for (post in post_list) {
                if (post !is Map<*, *>) {
                    throw APIException("Post was not a map! Post contents: $post, response: $response")
                }
                @Suppress("UNCHECKED_CAST") //Suppressing is okay because JSON is guaranteed to have string keys
                val body: String = post["text"] as String
                val num_upvotes: Number = post["vote_total"] as Number
                val num_comments: Number = post["comment_count"] as Number
                val created_at: String = post["created_at"] as String
                val image_url: String? = if ((post["assets"] as List<*>).isNotEmpty()) {
                    ((post["assets"] as List<*>)[0] as Map<*,*>).getOrDefault("url", null) as String?
                } else {
                    null //No image
                }
                new_post_list.add(Post(body, num_upvotes, image_url, num_comments, created_at))
            }
            memory_strings["cursor_${type.as_string()}"] = response["cursor"] as String
            val old_posts = memory_posts[type.as_string()]
            if (old_posts == null || old_posts.isEmpty()){
                //Log.d("Debug", "Old posts is null or empty, setting to new posts")
                memory_posts[type.as_string()] = new_post_list
            } else {
                //Log.d("Debug", "Old posts is not null or empty, adding to old posts")
                //Log.d("Debug", "Old posts size: ${old_posts.size}")
                memory_posts[type.as_string()] = old_posts + new_post_list
                //Log.d("Debug", "New posts size: ${new_post_list.size}")
                //Log.d("Debug", "memory_posts size: ${memory_posts[type.as_string()]!!.size}")
            }
        }

        fun check_email_verification(): Boolean {
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

        fun register_email(email: String): String {
            Log.d("Debug", "Deleteme: token is: $token")
            val get_response = get("https://api.sidechat.lol/v1/groups/login_type?email=$email", token)
            Log.d("Debug", "Get Response: $get_response")
            if ((get_response["message"] as String?)?.isNotEmpty() == true) {
                return get_response["message"] as String
            } else if (get_response.isNotEmpty()) {
                throw APIException("email get failed: $get_response")
            }
            val post_response = post(
                "https://api.sidechat.lol/v2/users/register_email",
                mapOf("email" to email),
                token
            )
            Log.d("Debug", "Post Response: $post_response")
            if ((post_response["message"] as String?)?.isNotEmpty() == true) {
                return post_response["message"] as String
            } else if (post_response.isNotEmpty()) {
                throw APIException("email post falied: $post_response")
            }
            return "" //Return empty string to mark a no-error response
        }

        fun complete_registration(age: String) {
            val age_number = age.toInt()
            val url = "https://api.sidechat.lol/v1/complete_registration"
            val data: Map<String, Any> = mapOf(
                "registration_id" to memory_strings["registration_id"]!!,
                "age" to age_number,
            )
            val response = post(url, data)
            Log.d("Debug_API", "Registration Completion Response $response")
            val _token = response.getOrDefault("token", "") as String
            val user = response.getOrDefault("user", false)
            Log.d("Debug_API", "Token is: $token")
            Log.d("Debug_API", "User is: $user")
            if (_token.isEmpty() || _token.length < 64) {
                //bad token
                Log.d("Debug", "Bad Token!")
                throw APIException(response.toString())
            } else {
                token = _token
                longterm_put("token", token!!)
                Log.d("Debug_API", "Stored token into memory and longterm")
                //Need to register device token
                val device_id = getDeviceID()
                memory_strings["device_id"] = device_id
                //Need to use the token as a bearer token
                val device_token_url = "https://api.sidechat.lol/v1/register_device_token"
                val device_token_data = mapOf(
                    "build_type" to "release",
                    "bundle_id" to "com.flowerave.sidechat",
                    "device_token" to device_id,
                )
                val device_token_response = post(device_token_url, device_token_data, token.toString())
                Log.d("Debug_API", "device_token_response was: $device_token_response")
                if (device_token_response.isNotEmpty()) {
                    throw APIException(device_token_response.toString())
                }
            }
        }

        fun phone_verify(twofactor_code: String): SetupTwoFactor.Companion.TwoFactorResponse {
            val twofactor_code_upper = twofactor_code.uppercase()
            val url = "https://api.sidechat.lol/v1/verify_phone_number"
            val data = mapOf("code" to twofactor_code_upper, "phone_number" to memory_strings["phone_number"].toString())
            val response = post(url, data)
            Log.d("Debug_API", "Phone Number Verification Response $response")
            //If this is a new user, the response will include a registration_id and setup must continue
            //Otherwise, it includes a user object and a token
            val message = response.getOrDefault("message", false)
            if (message != false) {
                //This likely means authentication failed!
                latest_errmsg = message as String
                return SetupTwoFactor.Companion.TwoFactorResponse.Auth_Failed
            }
            val logged_in_user = response.getOrDefault("logged_in_user", false)
            if (logged_in_user == false) {
                val registration_id = response.getOrDefault("registration_id", false)
                if (registration_id == false) {
                    throw APIException(response.toString())
                } else {
                    memory_strings["registration_id"] = registration_id as String
                }
                return SetupTwoFactor.Companion.TwoFactorResponse.Auth_Required //Additional setup required, go to SetupAge
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
                } catch (e: Exception) {
                    Log.d("Debug", "logged_in_user - Error converting to Map: $logged_in_user")
                    throw APIException("logged_in_user - Error converting to map: $logged_in_user")
                }
                try {
                    @Suppress("UNCHECKED_CAST")
                    group = logged_in_user["group"] as Map<String, Any>
                } catch (e: Exception) {
                    Log.d("Debug", "No group found! Redoing email setup")
                    return SetupTwoFactor.Companion.TwoFactorResponse.Auth_Email_Required
                }
                token = logged_in_user["token"] as String
                if (token!!.isEmpty() || token!!.length < 64) {
                    //Bad token
                    throw APIException("Bad token recieved in phone_verify: $token [is null?: ${token == null}]")
                }
                Log.d("Debug_API", "Values retrieved! Token is: $token, group is $group, user is $user")
                parse_user(user)
                parse_group(group)
                longterm_put("token", token!!)
                return SetupTwoFactor.Companion.TwoFactorResponse.Auth_Complete //No additional setup required
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
                memory_strings["phone_number"] = phoneNumber_with_country_code
            }
        }

        fun getImageFromUrl(url: String, imageView: ImageView, bearer_token: String) {
            Log.d("Debug", "Getting image from $url with token: $bearer_token")
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $bearer_token")
                .build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // handle failure
                }

                override fun onResponse(call: Call, response: Response) {
                    val inputStream = response.body!!.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imageView.post {
                        imageView.setImageBitmap(bitmap)
                        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                }
            })
        }

        fun get_returnfuture(url: String, bearer_token: String?): FutureTask<Map<String, Any>> {
            Log.d("Debug_API", "Submitting GET request to $url")
            val get_callable = Callable {
                val responseBody: String = _get(url, bearer_token) as String
                Log.d("Debug", "Inside get_returnfuture, responseBody is: $responseBody")

                if (responseBody.isEmpty()) {
                    return@Callable mapOf()
                }
                if (responseBody.uppercase() == "UNAUTHORIZED") {
                    //RIP Unauthorized lol
                    return@Callable mapOf("message" to "Unauthorized -- Unknown Error.\n Try force-closing the app and re-opening it")
                }

                val mapper = jacksonObjectMapper()
                @Suppress("UNCHECKED_CAST") //Safe to assume string keys, because JSON can only have string keys
                val objData = mapper.readValue(responseBody, Map::class.java) as Map<String, Any>

                Log.d("Debug_API", "Parsed Response is: $objData")
                return@Callable objData
            }
            val future_callable = FutureTask(get_callable)
            val thread = Thread(future_callable)
            thread.start()
            return future_callable
        }

        fun get(url: String, bearer_token: String?): Map<String, Any> {
            //TODO: Figure out how to make this API call non-blocking (same issue as with post)
            val future_callable = get_returnfuture(url, bearer_token)
            val result = future_callable.get() //Blocks until the request is done :(
            Log.d("Debug_API", "Get Request complete to $url")
            return result
        }

        private fun _get(plaintext_url: String, bearer_token: String?): Any {
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
            return responseBody
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