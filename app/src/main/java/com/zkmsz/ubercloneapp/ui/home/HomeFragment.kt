package com.zkmsz.ubercloneapp.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.zkmsz.ubercloneapp.Common
import com.zkmsz.ubercloneapp.R
import java.lang.Exception

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var _mapFragment:SupportMapFragment

    //location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    //online system
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef:DatabaseReference
    private lateinit var driversLocationRef:DatabaseReference
    private lateinit var geoFire: GeoFire


    //delete the data if the user id disconnected
    private val onlineValueEventListener= object : ValueEventListener
    {
        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(_mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            //if we has it
            if (snapshot.exists())
            {
                //remove value for the location
                currentUserRef.onDisconnect().removeValue()
            }
        }
    }


    //when the activity is starting run
    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    //this fun is in onResume
    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        init()

        _mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        _mapFragment.getMapAsync(this)

        return root
    }

    //the init fun ction in the onCreate fun
    private fun init() {

        onlineRef= FirebaseDatabase.getInstance().getReference().child(".info/connected")
        driversLocationRef= FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCES)
        currentUserRef= FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCES).child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        geoFire= GeoFire(driversLocationRef)

        registerOnlineSystem()

        locationRequest= LocationRequest()
        locationRequest.apply {
            this.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            this.setFastestInterval(3000)
            this.interval= 5000
            this.setSmallestDisplacement(10f)
        }

        //take last location
        locationCallback=object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                //set the location on map
                val newPos= LatLng(locationResult!!.lastLocation.latitude,locationResult.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                //set the location to the geoFire
                //Update location
                geoFire
                    .setLocation(
                        FirebaseAuth.getInstance().currentUser!!.uid,
                        GeoLocation(
                            locationResult.lastLocation.latitude,
                            locationResult.lastLocation.longitude
                        )
                ){key: String?, error: DatabaseError? ->
                        if (error != null)
                        {
                            Snackbar.make(_mapFragment.requireView(),error.message, Snackbar.LENGTH_LONG).show()
                        }
                        else
                        {
                            Snackbar.make(_mapFragment.requireView(),"You 're online!", Snackbar.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(requireContext())

        if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
        }
        else
        {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),12)
        }


    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        //check the permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            //if we don't have permission
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),12)

            return
        }

        //when we have permission
        if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            //Enable button first
            mMap.isMyLocationEnabled= true
            mMap.uiSettings.isMyLocationButtonEnabled= true

            //when i click on button
            mMap.setOnMyLocationButtonClickListener {

                Toast.makeText(context,"clicked",Toast.LENGTH_LONG).show()

                //get last location
                fusedLocationProviderClient.lastLocation
                    .addOnFailureListener{
                        Toast.makeText(context,it.message, Toast.LENGTH_LONG).show()
                    }

                    .addOnSuccessListener {location->
                        val userLatLng= LatLng(location.latitude,location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f))
                    }
                true
            }

            //layout
            val locationButton=_mapFragment.view?.findViewById<View>("1".toInt())
                ?.parent as View
            locationButton.findViewById<View>("2".toInt())

            val params= locationButton.layoutParams as? LinearLayout.LayoutParams
            params?.gravity= Gravity.BOTTOM
            params?.gravity= Gravity.LEFT
            params?.bottomMargin= 50
        }









        //try to set map style
        try {
            val success= mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.uber_maps_style))

            if(!success)
            {
                Log.e("ErrorMap", "Map Style is not parsing")
            }
        }
        catch (e:Exception)
        {
            Log.e("ErrorMap", e.message!!)
        }


    }

    //when this activity is onDestroy//finish
    override fun onDestroy() {
        //stope location update
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        //remove location
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        //dont listen to the dataChange
        onlineRef.removeEventListener(onlineValueEventListener)
        super.onDestroy()
    }
}
