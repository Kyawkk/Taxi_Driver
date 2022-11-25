package com.codetest.taxidriver

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.codetest.taxidriver.databinding.ActivityLocationBinding
import com.codetest.taxidriver.utils.Constant
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class LocationActivity : AppCompatActivity(), OnMapReadyCallback,OnMapsSdkInitializedCallback {
    private lateinit var binding: ActivityLocationBinding
    private var longitude = 0.0
    private var latitude = 0.0
    private lateinit var profileUrl: String
    private lateinit var customerName: String
    private lateinit var driverName: String
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var REQUEST_TIME = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // get data from previous activity
        getDataFromIntent()

        title = customerName

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun getDataFromIntent() {
        longitude = intent.extras?.get(Constant.LONGITUDE) as Double
        latitude = intent.extras?.get(Constant.LATITUDE) as Double
        profileUrl = intent.extras?.get(Constant.PROFILE).toString()
        customerName = intent.extras?.get(Constant.CUSTOMER_NAME).toString()
        driverName = intent.extras?.get(Constant.DRIVER_NAME).toString()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Constant.getCurrentLocation(this, onReceivedLatLng = {
            val customerLocation = LatLng(latitude, longitude)
            val currentLocation = LatLng(it.latitude, it.longitude)

            googleMap.uiSettings.isCompassEnabled = true

            googleMap.addPolyline(
                PolylineOptions()
                    .add(
                        currentLocation,
                        customerLocation
                    )
                    .width(5f)
                    .color(Color.RED)
                    .geodesic(true)
            )

            googleMap.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title(driverName)
                    .snippet("Driver")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
            googleMap.addMarker(
                MarkerOptions()
                    .position(customerLocation)
                    .title(customerName)
                    .snippet("Customer")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    currentLocation,
                    10f
                )
            )

        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }


    override fun onMapsSdkInitialized(p0: MapsInitializer.Renderer) {
        // do nothing here
    }

}