package com.codetest.taxidriver.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.maps.android.SphericalUtil

object Constant {
    const val LOGGED_IN_CONSTANT = "loggedInConstant"
    const val PERSON = "person"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val PROFILE = "profile"
    const val CUSTOMER_NAME = "customerName"
    const val DRIVER_NAME = "driverName"
    var CURRENT_LATITUDE = 0.0
    var CURRENT_LONGITUDE = 0.0


    private var locationRequest = LocationRequest.create()
    fun getCurrentLocation(activity: Activity, onReceivedLatLng:(location: LatLng) -> Unit): LatLng{

        //initialize location request
        initializeLocationRequest()

        // check permission
        if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if (isGPSEnabled(activity)){
                LocationServices.getFusedLocationProviderClient(activity)
                    .requestLocationUpdates(locationRequest,object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult) {
                            super.onLocationResult(p0)

                            LocationServices.getFusedLocationProviderClient(activity)
                                .removeLocationUpdates(this)

                            if (p0 != null && p0.locations.size > 0){
                                val index = p0.locations.size - 1
                                val latitude = p0.locations.get(index).latitude
                                val longitude = p0.locations.get(index).longitude

                                onReceivedLatLng(LatLng(latitude, longitude))
                            }
                        }

                    },Looper.getMainLooper())
            }
            else{
                turnOnGPS(activity)
            }
        }
        else{
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),100)
        }

        return LatLng(0.0,0.0)
    }

    private fun initializeLocationRequest() {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 2000
    }

    private fun turnOnGPS(activity: Activity) {

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result = LocationServices.getSettingsClient(activity)
            .checkLocationSettings(builder.build())

        result.addOnCompleteListener {
            val response = it.result
        }
    }

    private fun isGPSEnabled(activity: Activity): Boolean{
        var locationManger : LocationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManger.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun getDistance(currentLocation: LatLng, customerLocation: LatLng): String{
        val distance = SphericalUtil.computeDistanceBetween(currentLocation,customerLocation)
        return String.format("%.2f", distance / 1000) + "km"
    }

    fun showMaterialDialog(activity: Activity, title:String, message: String, negTitle: String, posTitle: String, negAction:DialogInterface.OnClickListener?, posAction: DialogInterface.OnClickListener? ){
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(posTitle,posAction)
        builder.setNegativeButton(negTitle,negAction)
        builder.create().show()
    }

}