package com.example.starbucksmapapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
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

        // APIキーの存在確認
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "APIキーが設定されていません。local.propertiesを確認してください。", Toast.LENGTH_LONG).show()
            return
        }

        // Places APIの初期化
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)

        // マップフラグメントの初期化
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // スタンプ一覧ボタンの設定
        findViewById<android.view.View>(R.id.btn_stamps).setOnClickListener {
            startActivity(Intent(this, StampsActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // マップの設定
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        enableMyLocation()
    }

    private fun enableMyLocation() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        when {
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                map.isMyLocationEnabled = true
                loadStarbucksLocations()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
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

                // 現在地周辺のスターバックスを検索
                searchNearbyStarbucks(latLng)
            } ?: run {
                Toast.makeText(this, "現在地を取得できませんでした", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "位置情報の取得に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchNearbyStarbucks(location: LatLng) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.RATING, Place.Field.USER_RATINGS_TOTAL)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val placeResult = placesClient.findCurrentPlace(request)
        placeResult.addOnSuccessListener { response ->
            var starbucksCount = 0
            for (placeLikelihood in response.placeLikelihoods) {
                val place = placeLikelihood.place
                if (place.name?.contains("スターバックス", ignoreCase = true) == true ||
                    place.name?.contains("Starbucks", ignoreCase = true) == true) {
                    place.latLng?.let { pos ->
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .title(place.name)
                                .snippet("タップしてスタンプを獲得！")
                        )
                        marker?.tag = place.id
                        starbucksCount++
                    }
                }
            }

            if (starbucksCount == 0) {
                Toast.makeText(this, "近くにスターバックスが見つかりませんでした", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${starbucksCount}店舗のスターバックスを発見しました", Toast.LENGTH_SHORT).show()
            }

            // 情報ウィンドウのクリックリスナーを設定
            map.setOnInfoWindowClickListener { marker ->
                marker.tag?.let { tag ->
                    val placeId = tag as String
                    val placeName = marker.title ?: "Unknown Starbucks"

                    if (stampsManager.hasStamp(placeId)) {
                        Toast.makeText(this, "すでにこの店舗のスタンプを獲得済みです", Toast.LENGTH_SHORT).show()
                    } else {
                        stampsManager.addStamp(placeId, placeName)
                        Toast.makeText(this, "スタンプを獲得しました: $placeName", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Toast.makeText(this, "Places API エラー: ${exception.message}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "スターバックスの検索に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_LONG).show()
            }
        }
}