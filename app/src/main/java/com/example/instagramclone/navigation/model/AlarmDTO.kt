package com.example.instagramclone.navigation.model

data class AlarmDTO (
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,
    var kind : Int? = null,//어떤 종류의 알람인지
    var message : String? = null,
    var timestamp : Long? = null
)