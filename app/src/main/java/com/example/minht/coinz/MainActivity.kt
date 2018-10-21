package com.example.minht.coinz

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
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
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener,
        PermissionsListener,DownloadCompleteListener,NavigationView.OnNavigationItemSelectedListener {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var mDrawerLayout : DrawerLayout
    private var downloadDate = "" // Format: YYYY/MM/DD
    private var geoJsonString = "" // String with GeoJSON data
    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    private val MAX_MARKER_DISTANCE = 25 // Maximum distance from coin to collect it
    private val MAX_COINS = 50; // Maximum number of coins that can be collected on any day
    private var numDayCollectedCoins = 0; // Number of coins collected on the current day
    private var markerList = HashMap<String,Marker>() // Hashmap of markers shown in the map
    private var visitedMarkerIdList : Set<String> = setOf() // Set of markers already visited by user on the day
    private var walletList = mutableListOf<Coin>() // List of coins in user's wallet

    private val preferencesFile = "MyPrefsFile" // For storing preferences

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

    // Returns today's date in format: YYYY/MM/DD
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
            // Get current date
            val currDate = getCurrentDate()
            if (currDate.equals(downloadDate)) {
                // Already played today, render markers directly
                Log.d(tag,"[onMapReady] Already played today, rendering markers directly")
                renderJson(map,geoJsonString)
            } else {
                // First time playing today
                // Reset values and download coin map
                Log.d(tag,"[onMapReady] First time playing today, reset values and download map from server")
                downloadDate = currDate
                numDayCollectedCoins = 0
                visitedMarkerIdList = setOf()
                val downloadUrl = "http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson"
                Log.d(tag,"[onMapReady] Downloading from $downloadUrl")
                val downloadFileTask = DownloadFileTask(this)
                downloadFileTask.execute(downloadUrl)
            }
        }
    }

    private fun renderJson(map:MapboxMap?, geoJsonString: String) {
        if (map == null) {
            Log.d(tag, "[renderJson] map is null")
        } else {
            val featureCollection = FeatureCollection.fromJson(geoJsonString)
            val features = featureCollection.features()
            val numFeatures = features!!.size
            for (i in 0..numFeatures-1) {
                val feature = features.get(i)
                val featureGeom = feature.geometry()
                if (featureGeom is Point) {
                    val jsonObj = feature.properties()
                    if (jsonObj == null) {
                        Log.d(tag, "[renderJson] JSON object is null")
                    } else {
                        // Extract properties, show markers and populate marker list
                        val currency = jsonObj.get("currency").toString().replace("\"","")
                        val markerId = jsonObj.get("id").toString().replace("\"","")
                        if (visitedMarkerIdList.contains(markerId)) {
                            Log.d(tag,"[renderJson] Marker already visited, not rendering it")
                            continue
                        }
                        val approxVal =jsonObj.get("value").asFloat
                        val approxValFormat = String.format("%.2f",approxVal) // Round to 2 decimal digits for readability
                        val coordinatesList = featureGeom.coordinates()
                        val lat = coordinatesList[1]
                        val lng = coordinatesList[0]
                        val featureLatLng = LatLng(lat, lng)
                        val markerOpts = MarkerOptions().title(approxValFormat).snippet(currency).position(featureLatLng)
                        Log.d(tag, "[renderJson] marker was added into the map and into markerList\n")
                        val marker = map.addMarker(markerOpts)
                        markerList.put(markerId,marker)
                    }
                }
            }
            Log.d(tag, "[renderJson] all markers added and markerList populated")
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

    // When location changes,
    // If user is sufficiently close to any coin (at least 25m), remove it from the map
    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(originLocation)
            var removeMarkerId : String? = null
            // Compute distance to markers and act accordingly
            for ((markerId, marker) in markerList) {
                val distToMarker = distanceToMarker(originLocation,marker)
                // If user sufficiently close, remove marker from map
                if (distToMarker <= MAX_MARKER_DISTANCE) {
                    Log.d(tag,"[onLocationChanged] Within collection distance of marker " +
                            "with id $markerId distance = $distToMarker)")
                    map!!.removeMarker(marker)
                    visitedMarkerIdList += markerId
                    removeMarkerId = markerId
                    val coin = Coin(marker.snippet, marker.title.toDouble())
                    walletList.add(coin)
                    break
                }
            }
            markerList.remove(removeMarkerId)
            if (removeMarkerId != null) {
                Log.d(tag,"[onLocationChanged] Within collection distance of marker " +
                        "with id $removeMarkerId removed from map and markerList")
            }
            numDayCollectedCoins++
            // If all coins collected,
            if (numDayCollectedCoins == MAX_COINS) {
                openCollectAllCoinsDialog()
            }
        }
    }
    // Computes distance between the user and a marker using Haverside's formula
    private fun distanceToMarker(location: Location, marker : Marker) : Double {
        val userLat = Math.toRadians(location.latitude)
        val markerLat = Math.toRadians(marker.position.latitude)
        val latDiff = Math.toRadians(marker.position.latitude - location.latitude)
        val lngDiff = Math.toRadians(marker.position.longitude - location.longitude)
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
        + Math.cos(userLat) * Math.cos(markerLat) * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        val earthRadius = 6378000
        return earthRadius * c
    }

    // Opening information dialog when user collects all coins on the day
    private fun openCollectAllCoinsDialog() {
        val allCoinsDialog = AlertDialog.Builder(this)
        allCoinsDialog.setTitle("Collected all coins!")
        allCoinsDialog.setMessage("Congratulations, you have successfully collected all coins today!" +
         " You'll receive __ GOLD into your bank account as a reward."
                + " Play again tomorrow to get another daily bonus.")
        // Clicking OK dismisses the dialog
        allCoinsDialog.setPositiveButton("OK") { _: DialogInterface?, _: Int -> }
        allCoinsDialog.show()
    }

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
         // Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // Get saved values
        downloadDate = settings.getString("lastDownloadDate", "")
        geoJsonString = settings.getString("lastCoinMap","")
        numDayCollectedCoins = settings.getString("numDayCollectedCoins","0").toInt()
        visitedMarkerIdList = settings.getStringSet("visitedMarkers", setOf())
        // Write a message to ”logcat” (for debugging purposes)
        Log.d(tag, "[onStart] Recalled lastDownloadDate is $downloadDate")
        Log.d(tag, "[onStart] Recalled lastCoinMap is $geoJsonString")
        Log.d(tag, "[onStart] Recalled numDayCollectedCoins is $numDayCollectedCoins")
        Log.d(tag, "[onStart] Recalled visited markers:\n")
        for (visitedMarkerId in visitedMarkerIdList) {
            Log.d(tag, visitedMarkerId)
        }
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
        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
        Log.d(tag, "[onStop] Storing lastCoinMap as $geoJsonString")
        Log.d(tag, "[onStop] Number of collected coins as $numDayCollectedCoins")
        // Store values for persistence
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("lastCoinMap",geoJsonString)
        editor.putString("numDayCollectedCoins", numDayCollectedCoins.toString())
        editor.putStringSet("visitedMarkers", visitedMarkerIdList)
        Log.d(tag, "[onStop] Storing visited markers:\n")
        for (visitedMarkerId in visitedMarkerIdList) {
            Log.d(tag, visitedMarkerId)
        }
        editor.apply()
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
        Log.d(tag, "[downloadComplete] successfully extracted the String with GeoJSON data $geoJsonString")
        // Render markers after download was completed
        renderJson(map, geoJsonString)
    }

    // Handle navigation drawer click events
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Provisionally only handle sign out action which signs out current user and returns him to log in screen
        when (item.itemId) {
            R.id.sign_out -> {
                // Confirmation dialog for user to confirm this action
                val confirmSignOut = AlertDialog.Builder(this)
                confirmSignOut.setTitle("Confirm sign out")
                confirmSignOut.setMessage("Are you sure that you want to sign out from the game?")
                confirmSignOut.setCancelable(false)
                confirmSignOut.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                    // If user confirms action, he's signed out
                    Log.d(tag,"[onNavigationItemSelected] Signing out user")
                    FirebaseAuth.getInstance().signOut()
                    finish()
                    startActivity(Intent(this,LoginActivity::class.java))
                }
                // Otherwise nothing happens
                confirmSignOut.setNegativeButton("No") { _: DialogInterface?, _: Int -> }
                confirmSignOut.show()
            }
        }
        return true
    }
}