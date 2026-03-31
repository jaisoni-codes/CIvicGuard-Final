package com.js11.p_cubspolice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val LOCATION_REQUEST_PERMISSION = 106

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    var db: DatabaseReference? = null
    var geofire: GeoFire? = null
    var geoQuery: GeoQuery? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        p_swipeToRefresh.setColorSchemeColors(Color.parseColor("#528AAE"))

        p_swipeToRefresh.setOnRefreshListener{
            try {
                p_swipeToRefresh.isRefreshing = false
                overridePendingTransition(0, 0)
                finish()
                startActivity(intent)
                overridePendingTransition(0, 0)
            }catch (e : Exception){
                e.printStackTrace()
            }
        }

        rv_police.layoutManager = LinearLayoutManager(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!isLocationPermissionsAllowed()) {
            requestLocationPermission()
        }else{
            val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            val isConnectedToInternet: Boolean = activeNetwork?.isConnected == true

            if(isConnectedToInternet) {
                fetchLocation()
            }else{
                Snackbar.make(p_swipeToRefresh,
                    "Internet Not Available",
                    Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**Retrieving Reported CUBS from firebase*/

    /**START*/
    private fun getData(latitude: Double, longitude: Double) {
        db = FirebaseDatabase.getInstance().getReference("REPORTED-CUBS")
        geofire = GeoFire(db!!.child("REPORTED-CUBS-LOCATION"))

        val r_cubs_al = ArrayList<DataModel>()

        geoQuery = geofire!!.queryAtLocation(GeoLocation(latitude, longitude), 3.0)

        try {
            geoQuery!!.addGeoQueryEventListener(object : GeoQueryEventListener {
                override fun onKeyEntered(key: String, location: GeoLocation) {
                    try {
                        db!!.child(key)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {

                                    val r_cubs: DataModel? =
                                        snapshot.getValue(DataModel::class.java)

                                    if (r_cubs != null) {
                                        rv_police.visibility = View.VISIBLE
                                        tvNoCUBSReported.visibility = View.GONE

                                        r_cubs_al.add(r_cubs)

                                        rv_police.adapter = ReportedCUBSAdapter(r_cubs_al)
                                    }else{
                                        rv_police.visibility = View.GONE
                                        tvNoCUBSReported.visibility = View.VISIBLE
                                    }
                                }

                                override fun onCancelled(firebaseError: DatabaseError) {
                                    println("The read failed: " + firebaseError.message)
                                }
                            })
                    }catch (e : Exception){
                        e.printStackTrace()
                    }
                }

                override fun onKeyExited(key: String) {
                    overridePendingTransition(0, 0)
                    finish()
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }

                override fun onKeyMoved(key: String, location: GeoLocation) {
                }

                override fun onGeoQueryReady() {
                }

                override fun onGeoQueryError(error: DatabaseError) {
                }
            })

        }catch (e : Exception){
            e.printStackTrace()
        }
    }
    /**END*/


    /**Location related methods*/

    /**START*/
    private fun isLocationPermissionsAllowed() : Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermission()
            } else {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        try {
                            if (location != null) {
                                getData(location.latitude, location.longitude)
                            } else {
                                buildAlertDialog("The app couldn't get your location. Try again in few seconds..")
                            }
                        } catch (e: Exception) {
                            buildAlertDialog("The app couldn't get your location. Try again in few seconds..")
                        }
                    }
                    .addOnCanceledListener {
                        buildAlertDialog("The app couldn't get your location. Try again in few seconds..")
                    }
                    .addOnFailureListener {
                        buildAlertDialog("The app couldn't get your location. Try again in few seconds..")
                    }
            }
        } catch (e: Exception) {
            buildAlertDialog("The app couldn't get your location. Try again in few seconds..")
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_PERMISSION) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                fetchLocation()
            } else {
                buildAlertDialog("Location Permission Denied. Allow it from the settings to allow the app to function properly.")
            }
        }
    }

    /**Building Alert Dialogue*/

    private fun buildAlertDialog(message: String) {
        val builder: androidx.appcompat.app.AlertDialog.Builder =
            androidx.appcompat.app.AlertDialog.Builder(
                this
            )
        builder.setMessage(message)
            .setCancelable(true)
            .setNegativeButton("OK") { dialog, id -> dialog.cancel() }

        val alert: androidx.appcompat.app.AlertDialog = builder.create()
        alert.show()
    }

}
