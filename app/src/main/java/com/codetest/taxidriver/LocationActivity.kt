package com.codetest.taxidriver

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.codetest.taxidriver.databinding.ActivityLocationBinding
import com.codetest.taxidriver.utils.Constant
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions


class LocationActivity : AppCompatActivity(), OnMapReadyCallback, OnMapsSdkInitializedCallback {
    private lateinit var binding: ActivityLocationBinding
    private var longitude = 0.0
    private var latitude = 0.0
    private lateinit var profileUrl: String
    private lateinit var customerName: String
    private lateinit var driverName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)

        with(window){
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        }

        setContentView(binding.root)

        // get data from previous activity
        getDataFromIntent()

        //set activity title
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
                PolylineOptions().add(
                    currentLocation, customerLocation
                ).width(5f).color(Color.RED).geodesic(true)
            )

            googleMap.addMarker(
                MarkerOptions().position(currentLocation).title(driverName).snippet("Driver")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
            googleMap.addMarker(
                MarkerOptions().position(customerLocation).title(customerName).snippet("Customer")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    currentLocation, 10f
                )
            )

        })
    }
    override fun onMapsSdkInitialized(p0: MapsInitializer.Renderer) {
        // do nothing here
    }

}