package com.example.instagramclone.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagramclone.LoginActivity
import com.example.instagramclone.MainActivity
import com.example.instagramclone.R
import com.example.instagramclone.navigation.model.AlarmDTO
import com.example.instagramclone.navigation.model.ContentDTO
import com.example.instagramclone.navigation.model.FollowDTO
import com.example.instagramclone.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {

    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserId : String? = null

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth?.currentUser?.uid

        if(uid == currentUserId){
            //My Page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //Other User Page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainActivity = (activity as MainActivity)
            mainActivity?.toolbar_user_id?.text = arguments?.getString("userId")
            mainActivity?.toolbar_btn_back?.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainActivity?.toolbar_title_image?.visibility = View.GONE
            mainActivity?.toolbar_user_id?.visibility = View.VISIBLE
            mainActivity?.toolbar_btn_back?.visibility = View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    //????????? ?????????, ????????? ??? ??????
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFireStoreException ->

            if(documentSnapshot == null) return@addSnapshotListener

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }

            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserId!!)){
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserId){
                        //?????? ?????? ????????????
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    fun requestFollow(){
        //?????? ???????????? ????????? ????????? ?????????
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserId!!)
        firestore?.runTransaction {transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                //????????? ??? ?????? -> ???????????????
                followDTO?.followingCount = followDTO?.followingCount -1
                followDTO?.followers?.remove(uid)
            }else{
                //????????? ?????? ?????? -> ?????????
                followDTO?.followingCount = followDTO?.followingCount +1
                followDTO?.followers[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        //????????? ???????????? ????????? ????????? ????????? ?????????
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserId!!] = true
                followerAlarm(uid!!)

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserId)){
                //???????????? ???????????? -> ??????
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserId!!)
            }else{
                //????????? ??????????????? -> ?????????
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserId!!] = true
                followerAlarm(uid!!)
            }

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = auth?.currentUser?.email + " " + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "InstagramClone", message)
    }

//    //?????? ?????? ?????? ??? ????????? ????????? ?????????
//            //?????? ????????? ?????? ???
//            val image = "https://firebasestorage.googleapis.com/v0/b/instagramclone-d76d7.appspot.com/o/userProfileImages%2FxSVsQQFZJeholpRDVKZhm9xQWHV2?alt=media&token=ecf6b52b-3cf7-4c0c-b2ba-90f41b3966c1"
//            Glide.with(activity!!).load(image).apply(RequestOptions().circleCrop()).into(viewholder.detailviewitem_profile_image)
//
//            firestore?.collection("profileImages")?.document(contentDTOs[position].uid.toString())?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
//                if(documentSnapshot?.data == null) return@addSnapshotListener
//                if(documentSnapshot?.data != null){
//                    //?????? ???????????? ?????? ?????? ????????????
//                    var url = documentSnapshot?.data!!["image"]
//                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView.account_iv_profile)
//                }
//            }


    fun getProfileImage(){
        val image = "https://firebasestorage.googleapis.com/v0/b/instagramclone-d76d7.appspot.com/o/userProfileImages%2FxSVsQQFZJeholpRDVKZhm9xQWHV2?alt=media&token=ecf6b52b-3cf7-4c0c-b2ba-90f41b3966c1"
        Glide.with(activity!!).load(image).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)

        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener

            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { value, error ->
                //Sometimes, This code return null of value(querySnapshot) when it signout
                if(value == null) return@addSnapshotListener

                //Get data
                for(snapshot in value.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }
    }
}