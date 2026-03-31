package com.js11.p_cubspolice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_reported_cubs_layout.view.*


class ReportedCUBSAdapter(private val modelArrayList : ArrayList<DataModel>?) : RecyclerView.Adapter<ReportedCUBSAdapter.ViewHolder>()
{
    lateinit var fusedLocationClient : FusedLocationProviderClient

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_reported_cubs_layout,
        parent,false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(parent.context)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = modelArrayList?.get(position)

        /*Picasso
            .get()
            .load(model!!.cub_imageURL)
            .placeholder(R.drawable.add_screen_image_placeholder)
            .into(holder.cub_image)*/

        Glide
            .with(holder.cub_image.context)
            .load(model!!.cub_imageURL)
            .placeholder(R.drawable.r_cubs_image_placeholder)
            .into(holder.cub_image)

        holder.title.text = model.title
        holder.description.text = model.description
        holder.date.text = model.date
        holder.time.text = model.time
        holder.location.text = model.location

        holder.driveToLocation.setOnClickListener {
            driveToLocation(it, model.latitude, model.longitude)
        }

        holder.completed.setOnClickListener {
            deleteData(model.id, model.cub_imageURL)
        }
    }

    override fun getItemCount(): Int {
        return modelArrayList!!.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cub_image = itemView.p_iv_cub_image
        val title = itemView.p_title
        val description = itemView.p_description
        val date = itemView.p_date
        val time = itemView.p_time
        val location = itemView.p_location
        val completed = itemView.p_completed
        val driveToLocation = itemView.p_driveToLocation
    }

    private fun driveToLocation(view: View, destinationLatitude: Double, destinationLongitude: Double){

        if (ActivityCompat.checkSelfPermission(
                view.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                view.context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        fusedLocationClient.lastLocation
                    .addOnSuccessListener { currentLocation: Location ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.co.in/maps/dir/${currentLocation.latitude},${currentLocation.longitude}/${destinationLatitude},${destinationLongitude}"))
                            view!!.context.startActivity(intent)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
    }

    private fun deleteData(id: String, cub_imageURL : String) {
        val dataRef = FirebaseDatabase.getInstance().getReference("REPORTED-CUBS").child(id)
        val geoRef = FirebaseDatabase.getInstance().getReference("REPORTED-CUBS").child("REPORTED-CUBS-LOCATION").child(id)

        val cub_image_ref = FirebaseStorage.getInstance().getReferenceFromUrl(cub_imageURL)

        dataRef.removeValue()
        geoRef.removeValue()

        cub_image_ref.delete()
    }

}
