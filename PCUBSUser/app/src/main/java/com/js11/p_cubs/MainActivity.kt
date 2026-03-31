package com.js11.p_cubs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    var LOCATION_REQUEST_PERMISSION = 101
    var latLong: ArrayList<Double>? = ArrayList()

    var geoFire: GeoFire? = null

    private val CAMERA_REQUEST_CODE = 1000;
    var FilePathUri: Uri? = null
    var currentPhotoPath: String? = null

    var storageReference: StorageReference? = null
    var databaseReference: DatabaseReference? = null

    var mainActivityIntent: Intent? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityIntent = Intent(this, MainActivity::class.java)

        swipeToRefresh.setColorSchemeColors(Color.parseColor("#528AAE"))

        swipeToRefresh.setOnRefreshListener {
            swipeToRefresh.isRefreshing = false
            startActivity(mainActivityIntent)
            finish()
            overridePendingTransition(0, 0)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        storageReference = FirebaseStorage.getInstance().getReference("REPORTED-CUBS-IMAGES")
        databaseReference = FirebaseDatabase.getInstance().getReference("REPORTED-CUBS")

        var isConnectedToInternet: Boolean = false
        try {
            val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            isConnectedToInternet = activeNetwork?.isConnected == true

            val lm: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (!isConnectedToInternet && !isLocationEnabled) {
                Snackbar.make(
                    swipeToRefresh,
                    "Internet And GPS Not Available",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (!isConnectedToInternet) {
                Snackbar.make(
                    swipeToRefresh,
                    "Internet Not Available",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (!isLocationEnabled) {
                Snackbar.make(
                    swipeToRefresh,
                    "GPS Not Available",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                fetchLocation()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sdf: SimpleDateFormat? = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf!!.format(Date())
        et_date.setText(currentDate)

        val stf: SimpleDateFormat? = SimpleDateFormat("hh:mm a")
        var currentTime = stf!!.format(Date())
        et_time.setText(currentTime.toString())

        fl_image.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED
            ) {
                //permission was not enabled
                val permission =
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                //show popup to request camera permission
                requestPermissions(permission, CAMERA_REQUEST_CODE)
            } else {
                //permission already granted
                openCamera()
            }
        }

        btn_post.setOnClickListener {
            if (isConnectedToInternet) {
                uploadReportedCUBS()
            } else {
                buildAlertDialog("Internet not available")
            }
        }
    }


    /**Upload Reported CUBS to firebase*/

    /**START*/
    private fun uploadReportedCUBS() {
        try {
            if (et_title.text.toString().isNotEmpty() && et_description.text.toString()
                    .isNotEmpty() &&
                et_date.text.toString().isNotEmpty() && et_location.text.toString()
                    .isNotEmpty() && et_location.text.toString() != "PERMISSION DENIED"
                && FilePathUri != null
            ) {

                val progressDialog = Dialog(this)

                progressDialog.setContentView(R.layout.posting_reported_cubs_progress_dialog)
                progressDialog.setCancelable(false)

                val title = et_title!!.text.toString().trim { it <= ' ' }
                val description = et_description!!.text.toString().trim { it <= ' ' }
                val date = et_date!!.text.toString().trim { it <= ' ' }
                val time = et_time!!.text.toString().trim { it <= ' ' }
                val location = et_location!!.text.toString().trim { it <= ' ' }

                val latitude = latLong?.get(0)
                val longitude = latLong!![1]

                val reportedCubsID = databaseReference!!.push().key

                progressDialog.show()

                val storageReference2 = storageReference!!.child(
                    reportedCubsID + "." + getImageFileExtension(FilePathUri)
                )

                storageReference2.putFile(FilePathUri!!)
                    .addOnSuccessListener { taskSnapshot ->

                        storageReference2.downloadUrl.addOnSuccessListener {
                            val r_cubs_info = UploadReportedCubs(
                                reportedCubsID,
                                title,
                                description,
                                date,
                                time,
                                location,
                                latitude,
                                longitude,
                                it.toString()
                            )

                            databaseReference!!.child(reportedCubsID!!)
                                .setValue(r_cubs_info)

                            geoFire = GeoFire(databaseReference!!.child("REPORTED-CUBS-LOCATION"))

                            geoFire!!.setLocation(
                                reportedCubsID,
                                GeoLocation(latitude!!, longitude)
                            )//Current latitude and longitude

                            progressDialog.dismiss()

                            Toast.makeText(
                                applicationContext,
                                "Posted Successfully",
                                Toast.LENGTH_LONG
                            ).show()

                            startActivity(mainActivityIntent)
                            finish()
                            overridePendingTransition(0, 0)
                        }
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        buildAlertDialog("Error in posting the details..")
                    }
            } else {
                buildAlertDialog("Please Fill all the details")
            }
        } catch (e: Exception) {
            buildAlertDialog("Error in Posting the details")
            e.printStackTrace()
        }
    }
    /**END*/


    /**Image related methods*/

    /**START*/
    private fun openCamera() {
        //val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        val fileName = "P-CUBS.$timeStamp"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        try{
            val imageFile: File = File.createTempFile(
                fileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
            )

            currentPhotoPath = imageFile.absolutePath

            val imageUri = FileProvider.getUriForFile(this,
                "com.js11.p_cubs",
            imageFile)

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }catch (e : IOException){
            e.printStackTrace()
            buildAlertDialog("The app couldn't get the image. Try again after sometime.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {
            try {
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)

                if(bitmap != null) {
                    FilePathUri = getCameraImageUri(
                        this,
                        bitmap
                    )

                    iv_cub_image.setImageBitmap(bitmap)
                }else{
                    buildAlertDialog("The app couldn't get the image. Try again after sometime.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                buildAlertDialog("The app couldn't get the image. Try again after sometime.")
            }
        }
    }

    private fun getImageFileExtension(uri: Uri?): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    private fun getCameraImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path: String =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }
    /**END*/


    /**Location related methods*/

    /**START*/
    fun getAddress(latitude: Double, longitude: Double): String? {
        try {
            val addresses: List<Address>
            val geocoder: Geocoder = Geocoder(this, Locale.getDefault())

            addresses = geocoder.getFromLocation(latitude, longitude, 1)

            /*val city = addresses[0].locality
            val state = addresses[0].adminArea
            val country = addresses[0].countryName
            val postalCode = addresses[0].postalCode
            val knownName = addresses[0].featureName**/

            return addresses[0].getAddressLine(0)
        } catch (e: Exception) {
            buildAlertDialog("The app couldn't get your location..Try again in few seconds..")
            e.printStackTrace()
            return null
        }
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
                            if(location != null) {
                                latLong!!.add(location!!.latitude)
                                latLong!!.add(location!!.longitude)

                                et_location.setText(
                                    getAddress(
                                        latLong!![0],
                                        latLong!![1]
                                    )
                                )
                            }else{
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
        }catch (e : Exception) {
        buildAlertDialog("The app couldn't get your location. Try again in few seconds..")
        }
    }

    private fun requestLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_PERMISSION
            )
        }
    }


    /**Request Permissions Result*/

    /**START*/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == LOCATION_REQUEST_PERMISSION){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
                fetchLocation()
            }else{
                buildAlertDialog("Location Permission Denied. Allow it from the settings to allow the app to function properly.")
            }
        }else if(requestCode == CAMERA_REQUEST_CODE){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
                openCamera()
            }else{
                buildAlertDialog("Camera Permission Denied. Allow it from the settings to allow the app to function properly.")
            }
        }
    }
    /**END*/


    /**Building ALert Dialog*/

    /**START*/
    private fun buildAlertDialog(message: String) {
        val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(
            this
        )
        builder.setMessage(message)
            .setCancelable(true)
            .setNegativeButton("OK") { dialog, id -> dialog.cancel() }

        val alert: androidx.appcompat.app.AlertDialog = builder.create()
        alert.show()

    }
    /**END*/
}