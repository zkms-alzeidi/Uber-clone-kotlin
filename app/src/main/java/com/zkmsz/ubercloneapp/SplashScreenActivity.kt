package com.zkmsz.ubercloneapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.ColorSpace
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.zkmsz.ubercloneapp.Model.DriverInfoModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object
    {
        private val LOGIN_REQUEST_CODE= 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference



    /**************************************************************************************************
     **************************************************************************************************
     * on start
     * here I set time for display splash screen
     */

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    //show the splash screen
    @SuppressLint("CheckResult")
    private fun delaySplashScreen() {
        //show the splash screen for 3 seconds
        Completable.timer(3,TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe {
                //register the user if it is not logout
                firebaseAuth.addAuthStateListener(listener)
            }

    }




    /**************************************************************************************************
     **************************************************************************************************
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        //run fun init
        init()

    }

    //if it has a current user or not
    private fun init() {

        //get the information for the driver
        database= FirebaseDatabase.getInstance()
        driverInfoRef= database.getReference(Common.DRIVER_INFO_REFERENCE)

        //the providers
        providers= Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener= FirebaseAuth.AuthStateListener { myFirebaseAuth->
            val user= myFirebaseAuth.currentUser

            //if it has a current user
            if(user != null)
            {
                //check if it has information in firebase
                checkUserFromFirebase()
            }

            //if has not a current user
            else
            {
                //show the login layout
                showLoginLayout()
            }
        }
    }

    //if it has information get it and go  to Home activity
    private fun checkUserFromFirebase() {
        //check if the driver has information in data base or not
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)

            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity,error.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        val model= dataSnapshot.getValue(DriverInfoModel::class.java)
                        gotToHomeActivity(model)
                    }
                    else
                    {

                        showRegisterLayout()
                    }
                }

            })
    }

    private fun gotToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser= model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder= AlertDialog.Builder(this,R.style.DialogTheme)
        val itemView= LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edt_first_name= itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name= itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number= itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val btn_continue=  itemView.findViewById<View>(R.id.btn_register) as Button

        //set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null
            && TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
        {
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //Event

        btn_continue.setOnClickListener {
            if(TextUtils.isDigitsOnly(edt_first_name.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter First Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else if(TextUtils.isDigitsOnly(edt_last_name.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter Last Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter Phone Number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else
            {
                val model= DriverInfoModel()
                model.firstName= edt_first_name.text.toString()
                model.lastName= edt_last_name.text.toString()
                model.phoneNumber= edt_phone_number.text.toString()
                model.rating= 0.0

                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{
                        Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        progress_bar.visibility= View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this,"Register successfully",Toast.LENGTH_LONG).show()
                        dialog.dismiss()

                        gotToHomeActivity(model)

                        progress_bar.visibility= View.GONE
                    }
            }

        }

    }

    //show the login layout if it has not register
    private fun showLoginLayout() {

        val authMethodPickerLayout= AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        //start activity
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            ,LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE)
        {
            val response= IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK)
            {
                val user= FirebaseAuth.getInstance().currentUser
            }

            else
            {
                Toast.makeText(this, ""+response!!.error!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }



    /**************************************************************************************************
     **************************************************************************************************
     * onStop
     */
    override fun onStop() {
        if(firebaseAuth != null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }
}
