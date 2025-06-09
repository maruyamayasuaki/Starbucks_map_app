package com.example.starbucksmapapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private val stampsManager by lazy { StampsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<android.view.View>(R.id.btn_stamps).setOnClickListener {
            startActivity(Intent(this, StampsActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableMyLocation()
        loadStarbucksLocations()
    }

    private fun enableMyLocation() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        when {
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                map.isMyLocationEnabled = true
            }
            shouldShowRequestPermissionRationale(permission) -> {}
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun loadStarbucksLocations() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))

                val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                val request = FindCurrentPlaceRequest.newInstance(placeFields)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@addOnSuccessListener
                }
                val placeResult = placesClient.findCurrentPlace(request)
                placeResult.addOnSuccessListener { response ->
                    for (placeLikelihood in response.placeLikelihoods) {
                        val place = placeLikelihood.place
                        if (place.name?.contains("Starbucks", ignoreCase = true) == true) {
                            place.latLng?.let { pos ->
                                map.addMarker(
                                    MarkerOptions().position(pos).title(place.name)
                                )?.tag = place.id
                            }
                        }
                    }
                    map.setOnInfoWindowClickListener { marker ->
                        marker.tag?.let { tag ->
                            stampsManager.addStamp(tag as String, marker.title ?: "")
                        }
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            }
        }
}
