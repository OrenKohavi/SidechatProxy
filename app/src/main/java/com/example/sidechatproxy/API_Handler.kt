package com.example.sidechatproxy

import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask


class API_Handler {
    companion object {
        fun login_register(phoneNumber: String) {
            //Send the API call to sidechat
            var url = "https://api.sidechat.lol/v1/login_register"
            var data = mapOf("version" to 3, "phone_number" to phoneNumber)
            var response = post(url, data)
            if (!response.isEmpty()) {
                throw APIException(response.toString())
            }
        }

        fun post(url: String, args: Map<String, Any>): Map<String, Any> {
            val post_callable = Callable {
                return@Callable _post(url, args)
            }
            val future_callable = FutureTask(post_callable)
            val thread = Thread(future_callable)
            thread.start()
            val result = future_callable.get() //Blocks until the request is done :(
            return result
        }

        fun _post(plaintext_url : String, args : Map<String, Any>): Map<String, Any> {
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

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            val responseBody = response.body!!.string()

            //Response
            Log.d("Debug_API", "Response Body: $responseBody")

            //we could use jackson if we got a JSON
            //val objData = mapperAll.readTree(responseBody)

            //println("My name is " + objData.get("name").textValue() + ", and I'm a " + objData.get("job").textValue() + ".")
            //TODO
            return mapOf<String, Any>("Done" to "yes")
        }
    }
}