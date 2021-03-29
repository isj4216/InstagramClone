package com.example.instagramclone.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagramclone.MainActivity
import com.example.instagramclone.R
import com.example.instagramclone.navigation.model.AlarmDTO
import com.example.instagramclone.navigation.model.ContentDTO
import com.example.instagramclone.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {

    //DB접근위해
    var firestore : FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        init {
            //DB접근 후 데이터 받아옴
            firestore?.collection("images")
                ?.orderBy("timestamp", Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, error ->
                contentDTOs.clear()
                contentUidList.clear()

                //Simetimes, This code return null of querySnapshot when it signout
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                //값이 새로고침 되도록
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView
            //UserId
            viewholder.detailviewitem_textview.text = contentDTOs[position].userId

            //Image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            //Explain of context
            viewholder.detailviewitem_explain.text = contentDTOs[position].explain

            //likes
            viewholder.detailviewitem_favorite_counter.text = "Likes " + contentDTOs!![position].favoriteCount


            //사진 올린 사람 별 프로필 이미지 뿌리기
            //기본 이미지 세팅 후
            val image = "https://firebasestorage.googleapis.com/v0/b/instagramclone-d76d7.appspot.com/o/userProfileImages%2FxSVsQQFZJeholpRDVKZhm9xQWHV2?alt=media&token=ecf6b52b-3cf7-4c0c-b2ba-90f41b3966c1"
            Glide.with(activity!!).load(image).apply(RequestOptions().circleCrop()).into(viewholder.detailviewitem_profile_image)

            firestore?.collection("profileImages")?.document(contentDTOs[position].uid.toString())?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot?.data == null) return@addSnapshotListener
                if(documentSnapshot?.data != null){
                    //유저 이미지가 있는 경우 넣어주기
                    var url = documentSnapshot?.data!!["image"]
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(viewholder.detailviewitem_profile_image)
                }
            }

            //좋아요 버튼에 이벤트
            viewholder.detailviewitem_favorite.setOnClickListener {
                favoriteEvent(position)
            }

            //좋아요 카운트 + 하트 색
            if(contentDTOs!![position].favorites.containsKey(uid)){
                //좋아요 클릭된 상태
                viewholder.detailviewitem_favorite.setImageResource(R.drawable.ic_favorite)
            }else{
                //좋아요 클릭하지 않은 상태
                viewholder.detailviewitem_favorite.setImageResource(R.drawable.ic_favorite_border)
            }

            //본인 게시글일 경우 삭제 버튼 보이기
            if(contentDTOs!![position].uid == uid){
                //자신의 게시물
                Log.e("mine", contentDTOs!![position].toString())
                Log.e("mine", contentDTOs!![position].uid.toString())
                viewholder.detailviewitem_delete.visibility = View.VISIBLE
            }else{
                //아닐 경우
                viewholder.detailviewitem_delete.visibility = View.GONE
            }

            //삭제 이벤트
            viewholder.detailviewitem_delete.setOnClickListener {
                Log.e("deleteClick", contentDTOs[position].timestamp.toString())
                firestore?.collection("images")?.document(contentDTOs[position].timestamp.toString())?.delete()
            }

            //profile 이미지 클릭 시 상대방 유저정보로 이동
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }

            viewholder.detailviewitem_comment.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)

                startActivity(intent)
            }
        }

        //좋아요 눌렀을 때!
        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction {
                transaction ->
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    //좋아요 눌려 있을 때(취소 가능하게)
                    contentDTO.favoriteCount = contentDTO.favoriteCount!! - 1
                    contentDTO.favorites.remove(uid)
                }else{
                    //좋아요 안눌려 있을 때(좋아요 가능하게)
                    contentDTO.favoriteCount = contentDTO.favoriteCount!! + 1
                    contentDTO.favorites[uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO)
            }
        }

        fun favoriteAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document(alarmDTO.timestamp.toString()).set(alarmDTO)

            //앱 푸시
            var message = FirebaseAuth.getInstance()?.currentUser?.email + " " +
                    getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "InstagramClone", message)
        }
    }
}