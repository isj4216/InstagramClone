package com.example.instagramclone.navigation.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.instagramclone.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.squareup.okhttp.*
import java.io.IOException

class FcmPush {
    //푸쉬를 전송해주는 클래스
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAXBvRVj0:APA91bFhdsyaV3Bh2mrS3Ni_-eAIx0kBWM5pPy00JaLHmv3XSzYNNeKZkOPxy0IAHtj5clh8l1REtGYD3WZOIF3v1IvkP_OaeQcwsJ8QawvSIXIC4IBA0L1wc2lO4Ej-cE-oyaG5UlUp"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null

    companion object{
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtoken").document(destinationUid).get().addOnCompleteListener { task ->
            if (task.isSuccessful){
                var token = task.result?.get("pushtoken").toString()
                Log.e("push", token)

                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=" + serverKey)
                    .url(url)
                    .post(body)
                    .build()

                Log.e("body", pushDTO.notification.body.toString())

                okHttpClient?.newCall(request)?.enqueue(object : Callback {
                    override fun onFailure(request: Request?, e: IOException?) {

                    }

                    override fun onResponse(response: Response?) {
                        Log.e("bodys", response?.body()?.string().toString())
                    }
                })
            }
        }
    }
}