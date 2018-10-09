package com.example.minht.coinz

import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var geoJsonString: String
    private var coinMapList = mutableListOf<Coin>()  // Coins in the map
    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        Mapbox.getInstance(this, getString(R.string.access_token))

        // Need findViewById for a com.mapbox.mapboxsdk.maps.MapView
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
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
            geoJsonString = "{\n" +
                    "  \"type\": \"FeatureCollection\",\n" +
                    "  \"date-generated\": \"Tue Jan 01 2019\",\n" +
                    "  \"time-generated\": \"00:00\",\n" +
                    "  \"approximate-time-remaining\": \"23:59\",\n" +
                    "  \"rates\": {\n" +
                    "                   \"SHIL\": 14.549987669178702,\n" +
                    "                   \"DOLR\": 52.611565218628485,\n" +
                    "                   \"QUID\": 18.751726260433337,\n" +
                    "                   \"PENY\": 42.61827189254482\n" +
                    "               },\n" +
                    "  \"features\": [\n" +
                    "    {\n" +
                    "      \"type\": \"Feature\",\n" +
                    "      \n" +
                    "      \"properties\": {\n" +
                    "        \"id\": \"9479-38a9-1660-7b9c-d091-7279\",\n" +
                    "        \"value\": \"1.629179461619984\",\n" +
                    "        \"currency\": \"SHIL\",\n" +
                    "        \"marker-symbol\": \"1\",\n" +
                    "        \"marker-color\": \"#0000ff\"\n" +
                    "      },\n" +
                    "      \n" +
                    "      \"geometry\": {\n" +
                    "        \"type\": \"Point\",\n" +
                    "        \"coordinates\": [\n" +
                    "          -3.1903031429467235,\n" +
                    "          55.9446544923207\n" +
                    "        ]\n" +
                    "      }\n" +
                    "\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"Feature\",\n" +
                    "      \n" +
                    "      \"properties\": {\n" +
                    "        \"id\": \"db19-fa94-a852-8d13-81ba-1f7d\",\n" +
                    "        \"value\": \"8.931103513894806\",\n" +
                    "        \"currency\": \"PENY\",\n" +
                    "        \"marker-symbol\": \"9\",\n" +
                    "        \"marker-color\": \"#ff0000\"\n" +
                    "      },\n" +
                    "      \n" +
                    "      \"geometry\": {\n" +
                    "        \"type\": \"Point\",\n" +
                    "        \"coordinates\": [\n" +
                    "          -3.1893463819968977,\n" +
                    "          55.944287482364615\n" +
                    "        ]\n" +
                    "      }\n" +
                    "\n" +
                    "    }\n" +
                    "        \n" +
                    "   ]\n" +
                    "}" // String for testing purposes
            // Render markers
            renderJson(map, geoJsonString)
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
                        val coinId = jsonObj.get("id").toString().replace("\"","")
                        val approxVal =jsonObj.get("value").asFloat
                        val approxValFormat = String.format("%.2f",approxVal) // Round to 2 decimal digits for readability
                        val coordinatesList = featureGeom.coordinates()
                        val lat = coordinatesList[1]
                        val lng = coordinatesList[0]
                        val featureLatLng = LatLng(lat, lng)
                        val coin = Coin(coinId,currency,featureLatLng)
                        coinMapList.add(coin) // Add coin to list of coins in the map
                        Log.d(tag, "[renderJson] coin " + coin.toString() + " was added")
                        map.addMarker(MarkerOptions().title(approxValFormat).snippet(currency).position(featureLatLng))
                    }
                }
            }
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
            for (coin in coinMapList) {
                val distance = distanceCoin(location, coin)
                Log.d(tag, "[onLocationChanged] Distance to " + coin.toString() + ": $distance\n")
            }
            setCameraPosition(originLocation)
        }
    }

    // Computes distance between the user and the coin is computed using Haverside's formula
    private fun distanceCoin(location: Location, coin: Coin) : Double {
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
}