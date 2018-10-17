package com.example.minht.coinz

import android.content.Intent
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import kotlinx.android.synthetic.main.activity_main.*
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener,
        PermissionsListener,DownloadCompleteListener,NavigationView.OnNavigationItemSelectedListener {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    //private var coinMapList = mutableListOf<Coin>()  // Coins in the map
    private lateinit var currentDate: String // Format: YYYY/MM/DD
    private lateinit var geoJsonString: String
    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin
    private lateinit var mDrawerLayout : DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        Mapbox.getInstance(this, getString(R.string.access_token))

        // Need findViewById for a com.mapbox.mapboxsdk.maps.MapView
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    // Returns todays date in format: YYYY/MM/DD
    private fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("yyyy/MM/dd",java.util.Locale.getDefault())
        val result = sdf.format(Date())
        Log.d(tag, "[getCurrentDate]: current date is $result")
        return result
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            // Make location information available
            enableLocation()
            // Set up URL to download map from
            currentDate = getCurrentDate()
            val downloadUrl = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
            Log.d(tag,"[onMapReady] downloading from $downloadUrl")
            val downloadFileTask = DownloadFileTask(this)
            downloadFileTask.execute(downloadUrl)
        }
    }

    private fun renderJson(map:MapboxMap?, geoJsonString: String) {
        if (map == null) {
            Log.d(tag, "[renderJson] map is null")
        } else {
            val featureCollection = FeatureCollection.fromJson(geoJsonString)
            val features = featureCollection.features()
            features!!.forEach { feature ->
                val featureGeom = feature.geometry()
                if (featureGeom is Point) {
                    val jsonObj = feature.properties()
                    if (jsonObj == null) {
                        Log.d(tag, "[renderJson] JSON object is null")
                    } else {
                        // Extract properties and show marker
                        val currency = jsonObj.get("currency").toString().replace("\"","")
                        //val coinId = jsonObj.get("id").toString().replace("\"","")
                        val approxVal =jsonObj.get("value").asFloat
                        val approxValFormat = String.format("%.2f",approxVal) // Round to 2 decimal digits for readability
                        val coordinatesList = featureGeom.coordinates()
                        val lat = coordinatesList[1]
                        val lng = coordinatesList[0]
                        val featureLatLng = LatLng(lat, lng)
                        //val coin = Coin(coinId,currency,featureLatLng)
                        //coinMapList.add(coin) // Add coin to list of coins in the map
                        map.addMarker(MarkerOptions().title(approxValFormat).snippet(currency).position(featureLatLng))
                    }
                }
            }
            Log.d(tag, "[renderJson] all markers added")
        }
    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        locationEngine = LocationEngineProvider(this)
                .obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        if (mapView == null) {
            Log.d(tag, "mapView is null")
        } else {
            if (map == null) {
                Log.d(tag, "map is null")
            } else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!,
                        map!!, locationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        Log.d(tag, "[setCameraPosition] Current position: Lat: ${location.latitude} Lng: ${location.longitude}")
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            originLocation = location
            // Report distance from user to each coin (for testing purposes)
            /*for (coin in coinMapList) {
                val distance = distanceCoin(location, coin)
                Log.d(tag, "[onLocationChanged] Distance to " + coin.toString() + ": $distance\n")
            }*/
            setCameraPosition(originLocation)
        }
    }

    // Computes distance between the user and the coin is computed using Haverside's formula
    /*private fun distanceCoin(location: Location, coin: Coin) : Double {
        val userLat = Math.toRadians(location.latitude)
        val coinLat = Math.toRadians(coin.position.latitude)
        val latDiff = Math.toRadians(coin.position.latitude - location.latitude)
        val lngDiff = Math.toRadians(coin.position.longitude - location.longitude)
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
        + Math.cos(userLat) * Math.cos(coinLat) * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        val earthRadius = 6378000
        val distance = earthRadius * c
        return distance
    }*/

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain:
                                     MutableList<String>?) {
        Log.d(tag, "Permissions: $permissionsToExplain")
        // Present popup message or dialog
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
        }
    }

    public override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    public override fun onResume() {
        super.onStart()
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    public override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }

    override fun downloadComplete(result: String) {
        geoJsonString = result
        Log.d(tag, "[downloadComplete] successfully extracted the String with GeoJSON data")
        // Render markers after download was completed
        renderJson(map, geoJsonString)
    }

    // Handle navigation drawer click events
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Provisionally only handle sign out action which signs out current user and returns him to log in screen
        when (item.itemId) {
            R.id.sign_out -> {
                Log.d(tag,"[onNavigationItemSelected] Signing out user")
                FirebaseAuth.getInstance().signOut();
                finish()
                startActivity(Intent(this,LoginActivity::class.java))
            }
        }
        return true;
    }
}