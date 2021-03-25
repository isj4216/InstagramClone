package com.example.instagramclone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email_login_btn.setOnClickListener {
            signInAndSignUp()
        }

        google_sign_in_btn.setOnClickListener {
            //구글 로그인 1단계
            googleLogin()
        }

        facebook_login_btn.setOnClickListener {
            //Facebook 로그인 1단계
           facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
//        printHashKey()
        callbackManager = CallbackManager.Factory.create()
    }

    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    //Y/9SHNfvoTOv/P+tBSUWTXnNwdk=
    /*************************************FACEBOOK*************************************************/
    fun printHashKey(){
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }
    }

    fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    //Facebook 로그인 2단
                    handleFacebookAccessToken(result?.accessToken)
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {

                }
            })
    }

    fun handleFacebookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if (task.isSuccessful) {
                    //Facebook 로그인 3단
                    //아이디와 패스워드가 맞았을때
                    Toast.makeText(this, "로그인 완료", Toast.LENGTH_LONG).show()
                    moveMainPage(task.result?.user)
                }else {
                    //틀렸을때
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }
    /*************************************FACEBOOK*************************************************/

    /**************************************GOOGLE**************************************************/
    fun googleLogin(){
        //구글 로그인
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*************************************FACEBOOK*********************************************/
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        /*************************************FACEBOOK*********************************************/

        /**************************************GOOGLE**********************************************/
        if(requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result!!.isSuccess){
                var account = result.signInAccount
                //구글 로그인 2단계
                firebaseAuthWithGoogle(account)
            }
        }
        /**************************************GOOGLE**********************************************/
    }

    fun firebaseAuthWithGoogle(account : GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if (task.isSuccessful) {
                    //아이디와 패스워드가 맞았을때
                    Toast.makeText(this, "로그인 완료", Toast.LENGTH_LONG).show()
                    moveMainPage(task.result?.user)
                }else {
                    //틀렸을때
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }
    /**************************************GOOGLE**************************************************/

    fun signInAndSignUp(){

        val email = email_edittext.text.toString()
        val password = password_edittext.text.toString()

        if(email.trim().isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_LONG).show()
            email_edittext.setText("")
        }
        else if(password.trim().isEmpty()) {
            Toast.makeText(this, "패스워드를 입력하세요", Toast.LENGTH_LONG).show()
            password_edittext.setText("")
        }
        else{
            //회원가입
            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener {
                        task ->
                    if(task.isSuccessful){
                        //아이디가 생성 되었을 때
                        Toast.makeText(this, "아이디 생성 및 로그인 완료", Toast.LENGTH_LONG).show()
                        moveMainPage(task.result?.user)
                    }else{
                        //로그인
                        signInEmail()
                    }
                }
        }
    }

    fun signInEmail(){
        //로그인 로직
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {
                task ->
                    if (task.isSuccessful) {
                        //아이디와 패스워드가 맞았을때
                        Toast.makeText(this, "로그인 완료", Toast.LENGTH_LONG).show()
                        moveMainPage(task.result?.user)
                    }else {
                        //틀렸을때
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
            }
    }

    fun moveMainPage(user:FirebaseUser?){
        //로그인 성공시 다음페이지로 이동
        if(user != null){
            //유저가 있을경우 메인페이지로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}