package com.zkmsz.ubercloneapp.Utils

import android.content.Context
import android.view.TouchDelegate
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.FirebaseDatabase
import com.zkmsz.ubercloneapp.Common

object UserUtils {
    fun updateUse(context:Context , updateData: HashMap<String,Any>)
    {
        FirebaseDatabase.getInstance().getReference(Common.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener{
                Toast.makeText(context,it.message!!,Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Toast.makeText(context,"Update information success",Toast.LENGTH_LONG).show()
            }
    }
}