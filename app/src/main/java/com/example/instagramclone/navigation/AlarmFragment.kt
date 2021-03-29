package com.example.instagramclone.navigation

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagramclone.R
import com.example.instagramclone.navigation.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class AlarmFragment : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm, container, false)
        view.alarm_recyclerview.adapter = AlarmRecyclerViewAdapter()
        view.alarm_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid", uid).addSnapshotListener { value, error ->
                alarmDTOList.clear()
                if(value == null) return@addSnapshotListener

                for(snapshot in value.documents){
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)

            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView

            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener {task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]
                    Glide.with(view.context).load(url).apply(RequestOptions().circleCrop()).into(view.comment_profile_imageview)
                }
            }

            when(alarmDTOList[position].kind){
                0 -> {
                    var str_0 = alarmDTOList[position].userId + " " + getString(R.string.alarm_favorite)
                    view.comment_profile_textview.text = str_0
                }

                1 -> {
                    var str_1 = alarmDTOList[position].userId + " " + getString(R.string.alarm_comment)
                    view.comment_profile_textview.text = str_1
                }

                2 -> {
                    var str_2 = alarmDTOList[position].userId + " " +getString(R.string.alarm_follow)
                    view.comment_profile_textview.text = str_2
                }
            }
            view.comment_message.visibility = View.GONE

            //알림 리스트 삭제
            holder.itemView.delete_btn.setOnClickListener {
                FirebaseFirestore.getInstance().collection("alarms").document(alarmDTOList[position].timestamp.toString()).delete()
            }
        }
    }
}