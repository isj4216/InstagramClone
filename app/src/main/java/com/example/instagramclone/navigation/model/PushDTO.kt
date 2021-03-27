package com.example.instagramclone.navigation.model

import android.app.Notification

data class PushDTO(
    var to : String? = null, //푸쉬 받는 사람의 토큰 아이디
    var notification : Notification = Notification()
){
    data class Notification(
        var body : String? = null, //푸쉬 메세지 주 내용
        var title : String? = null //푸쉬 메세지 제목
    )
}