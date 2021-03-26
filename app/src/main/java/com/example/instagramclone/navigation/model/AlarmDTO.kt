package com.example.instagramclone.navigation.model

data class AlarmDTO (
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,
    //0 -> 좋아요, 1 -> 댓글, 2 -> 팔로우
    var kind : Int? = null,//어떤 종류의 알람인지
    var message : String? = null,
    var timestamp : Long? = null
)