package com.example.starbucksmapapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var searchView: SearchView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchResultAdapter
    private val stampsManager by lazy { StampsManager(this) }
    private val markers = mutableListOf<Marker>()
    private var currentLocation: LatLng? = null

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

        // UIコンポーネントの初期化
        setupUI()

        // マップフラグメントの初期化
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupUI() {
        // 検索機能の設定
        searchView = findViewById(R.id.search_view)
        searchResultsRecyclerView = findViewById(R.id.search_results)

        searchAdapter = SearchResultAdapter(emptyList()) { store ->
            moveToStore(store)
            hideSearchResults()
        }

        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchStarbucks(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    hideSearchResults()
                } else if (newText.length >= 2) {
                    searchStarbucks(newText)
                }
                return true
            }
        })

        // スタンプ一覧ボタンの設定
        findViewById<android.view.View>(R.id.btn_stamps).setOnClickListener {
            startActivity(Intent(this, StampsActivity::class.java))
        }

        // 現在地ボタンの設定
        findViewById<android.view.View>(R.id.btn_current_location).setOnClickListener {
            currentLocation?.let { location ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            } ?: run {
                enableMyLocation()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // マップの設定
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false // カスタムボタンを使用

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

        enableMyLocation()
    }

    private fun enableMyLocation() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        when {
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                map.isMyLocationEnabled = true
                loadCurrentLocationAndStarbucks()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun loadCurrentLocationAndStarbucks() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                loadNearbyStarbucks(currentLocation!!)
            } ?: run {
                Toast.makeText(this, "現在地を取得できませんでした", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "位置情報の取得に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNearbyStarbucks(location: LatLng) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.RATING, Place.Field.USER_RATINGS_TOTAL)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val placeResult = placesClient.findCurrentPlace(request)
        placeResult.addOnSuccessListener { response ->
            clearMarkers()
            var starbucksCount = 0

            for (placeLikelihood in response.placeLikelihoods) {
                val place = placeLikelihood.place
                if (isStarbucks(place.name)) {
                    place.latLng?.let { pos ->
                        addStarbucksMarker(place, pos)
                        starbucksCount++
                    }
                }
            }

            if (starbucksCount == 0) {
                Toast.makeText(this, "近くにスターバックスが見つかりませんでした", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${starbucksCount}店舗のスターバックスを発見しました", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Toast.makeText(this, "Places API エラー: ${exception.message}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "スターバックスの検索に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchStarbucks(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
                val encodedQuery = URLEncoder.encode("$query スターバックス", "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=$encodedQuery&key=$apiKey"

                val response = URL(url).readText()
                val jsonObject = JSONObject(response)
                val results = jsonObject.getJSONArray("results")

                val stores = mutableListOf<StarbucksStore>()

                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val name = place.getString("name")

                    if (isStarbucks(name)) {
                        val placeId = place.getString("place_id")
                        val address = place.optString("formatted_address", "住所不明")
                        val geometry = place.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        stores.add(StarbucksStore(name, address, placeId, lat, lng))
                    }
                }

                withContext(Dispatchers.Main) {
                    if (stores.isNotEmpty()) {
                        showSearchResults(stores)
                    } else {
                        hideSearchResults()
                        Toast.makeText(this@MainActivity, "該当する店舗が見つかりませんでした", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideSearchResults()
                    Toast.makeText(this@MainActivity, "検索に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSearchResults(stores: List<StarbucksStore>) {
        searchAdapter.updateStores(stores)
        searchResultsRecyclerView.visibility = android.view.View.VISIBLE
    }

    private fun hideSearchResults() {
        searchResultsRecyclerView.visibility = android.view.View.GONE
    }

    private fun moveToStore(store: StarbucksStore) {
        val storeLocation = LatLng(store.latitude, store.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(storeLocation, 16f))

        // 既存のマーカーをクリア
        clearMarkers()

        // 選択された店舗のマーカーを追加
        val marker = map.addMarker(
            MarkerOptions()
                .position(storeLocation)
                .title(store.name)
                .snippet("タップしてスタンプを獲得！")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        marker?.tag = store.placeId
        markers.add(marker!!)

        // 周辺のスターバックスも表示
        loadNearbyStarbucks(storeLocation)
    }

    private fun addStarbucksMarker(place: Place, position: LatLng) {
        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(place.name)
                .snippet("タップしてスタンプを獲得！")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        marker?.tag = place.id
        markers.add(marker!!)
    }

    private fun clearMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }

    private fun isStarbucks(name: String?): Boolean {
        return name?.contains("スターバックス", ignoreCase = true) == true ||
                name?.contains("Starbucks", ignoreCase = true) == true
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