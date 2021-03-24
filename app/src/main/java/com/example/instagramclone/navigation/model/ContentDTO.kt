package com.example.instagramclone.navigation.model

data class ContentDTO(var explain : String? = null,//설명
                      var imageUrl : String? = null,//이미지 주소
                      var uid : String? = null,//어느 유저가 올렸는지
                      var userId : String? = null,//올린 유저 이미지 관리
                      var timestamp : Long? = null,//올린 시간
                      var favoriteCount : Int? = 0,//좋아요 누른 개수
                                                    //좋아요 누른 유저 관리(중복방지)
                      var favorites : MutableMap<String, Boolean> = HashMap()) {

    //댓글 관리
    data class Comment(var uid : String? = null,
                       var userId : String? = null,
                       var comment : String? = null,
                       var timestamp: Long? = null)
}