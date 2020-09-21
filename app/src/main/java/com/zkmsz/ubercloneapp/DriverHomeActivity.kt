package com.zkmsz.ubercloneapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.zkmsz.ubercloneapp.Utils.UserUtils
import java.lang.StringBuilder

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController:NavController


    private lateinit var img_avatar: ImageView
    private lateinit var imageUri: Uri
    private lateinit var waitingDialog:AlertDialog
    private lateinit var storageReference:StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    private fun init() {

        storageReference = FirebaseStorage.getInstance().getReference()

        waitingDialog= AlertDialog.Builder(this)
            .setMessage("Waiting...")
            .setCancelable(false).create()

        //when clicked on items Navigation
        navView.setNavigationItemSelectedListener {
            if(it.itemId == R.id.nav_sign_out)
            {
                val builder= AlertDialog.Builder(this)
                builder.apply {
                    setTitle("Sign out")
                    setMessage("Do you want to sign out?")
                    setNegativeButton("CANCEL", {dialogInterface, _ -> dialogInterface.dismiss() })
                    setPositiveButton("SIGN OUT"){dialogInterface, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent= Intent(this@DriverHomeActivity, SplashScreenActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }.setCancelable(false)

                    val  dialog= builder.create()
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(resources.getColor(android.R.color.holo_red_dark))

                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(resources.getColor(R.color.colorAccent))
                    }
                    dialog.show()
                }
            }

            true
        }

        //the data for the user
        val headerView= navView.getHeaderView(0)
        val txt_name= headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone= headerView.findViewById<View>(R.id.txt_phone) as TextView
        val txt_star= headerView.findViewById<View>(R.id.txt_star) as TextView
        img_avatar= headerView.findViewById<View>(R.id.img_avatar) as ImageView

        txt_name.setText(Common.buildWelcomeMessage())
        txt_phone.setText(Common.currentUser!!.phoneNumber)
        txt_star.setText(StringBuilder().append(Common.currentUser!!.rating))

        //if we have avatar
        if (Common.currentUser != null && Common.currentUser!!.avatar != null && !TextUtils.isEmpty(Common.currentUser!!.avatar))
        {
            Glide.with(this)
                .load(Common.currentUser!!.avatar)
                .into(img_avatar)
        }

        //get the image from gallery
        img_avatar.setOnClickListener {
            val intent= Intent()
            intent.apply {
                setType("image/*")
                setAction(Intent.ACTION_GET_CONTENT)
            }

            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //get the image as uri from gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if(data != null && data.data != null)
            {
                imageUri= data.data!!
                img_avatar.setImageURI(imageUri)

                showDialogUpload()
            }
        }

    }

    private fun showDialogUpload()
    {
        val builder= AlertDialog.Builder(this)
        builder.apply {
            setTitle("Change avatar")
            setMessage("Do you want to change avatar")
            setNegativeButton("CANCEL", {dialogInterface, _ -> dialogInterface.dismiss() })
            setPositiveButton("CHANGE")
            {dialogInterface, _ ->

                if (imageUri != null)
                {
                    waitingDialog.show()
                    val avatarFolder=
                        storageReference.child("avatar/"+ FirebaseAuth.getInstance().currentUser!!.uid)

                    avatarFolder.putFile(imageUri)
                        .addOnFailureListener{error->
                            Snackbar.make(drawerLayout,error.message!!,Snackbar.LENGTH_LONG).show()
                            waitingDialog.dismiss()
                        }
                        .addOnCompleteListener{task->
                            if (task.isSuccessful)
                            {
                                avatarFolder.downloadUrl.addOnSuccessListener {uri ->
                                    var update_data = HashMap<String,Any>()
                                    update_data.put("avatar", uri.toString())

                                    UserUtils.updateUse(this@DriverHomeActivity,update_data)
                                }
                            }
                            waitingDialog.dismiss()
                        }
                        .addOnProgressListener {
                            val progress= (100.0*it.bytesTransferred / it.totalByteCount)
                            waitingDialog.setMessage(StringBuilder("Uploading: ").append(progress).append("%"))
                        }
                }

            }.setCancelable(false)

            val  dialog= builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(resources.getColor(android.R.color.holo_red_dark))

                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(resources.getColor(R.color.colorAccent))
            }
            dialog.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object
    {
        const val PICK_IMAGE_REQUEST= 7272
    }
}
