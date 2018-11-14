package com.example.minht.coinz

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener,
        PermissionsListener,DownloadCompleteListener,NavigationView.OnNavigationItemSelectedListener {

    // Location variables
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var mDrawerLayout : DrawerLayout
    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    // Coin collection mechanism variables
    private var numDayCollectedCoins = 0; // Number of coins collected on the current day
    private var markerList = HashMap<String,Marker>() // Hashmap of markers shown in the map
    private var visitedMarkerIdList : MutableSet<String> = mutableSetOf() // Set of markers already visited by user on the day
    private var walletList : ArrayList<Coin> = ArrayList()  // Set of coins in user's wallet

    // Shared preferences and map downloading
    private val preferencesFile = "MyPrefsFile" // For storing preferences
    private var downloadDate = "" // Format: YYYY/MM/DD
    private var mapString = "" // String with map data
    private lateinit var mapJson : JSONObject // JSON object of map

    // Firebase/Firestore variables
    private lateinit var mAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var db : FirebaseFirestore

    // Bank Account
    private var bankBalance = 0
    private var bankAccount : ArrayList<BankTransfer> = ArrayList()

    // Exchange rates for currencies
    private var penyRate = 0.0
    private var dolrRate = 0.0
    private var quidRate = 0.0
    private var shilRate = 0.0

    // Constants
    companion object {
        const val TAG = "MainActivity" // Logging purposes
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY = "Username"
        const val EMAIL_KEY = "Email"
        const val WALLET_KEY = "Wallet"
        const val MAX_MARKER_DISTANCE = 25 // Maximum distance from coin to collect it
        const val MAX_DAILY_COINS = 50; // Maximum number of coins that can be collected on per day
        const val MAX_COINS_LIMIT = 1000;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
        mAuth = FirebaseAuth.getInstance()
        uid = mAuth.uid!!
        db = FirebaseFirestore.getInstance()
        setUpNavDrawer()
        Mapbox.getInstance(this, getString(R.string.access_token))

        // Need findViewById for a com.mapbox.mapboxsdk.maps.MapView
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    // Sets up nav drawer with custom header
    // Make it listen for menu item clicks
    private fun setUpNavDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val navUsernameText = headerView.findViewById(R.id.nav_text_username) as TextView
        val navEmailText = headerView.findViewById(R.id.nav_text_email) as TextView
        val userRef = db.collection(COLLECTION_KEY).document(uid)
        userRef.get().addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
            if (documentSnapshot!!.exists()) {
                val username = documentSnapshot.getString(USERNAME_KEY)
                val email = documentSnapshot.getString(EMAIL_KEY)
                val usernameText = "Welcome back $username!"
                navUsernameText.text = usernameText
                navEmailText.text = email
                Log.d(TAG,"[onCreate] Created welcome message")
            } else {
                Log.d(TAG,"[onCreate] User document not found")
            }
        }
        navigationView.setNavigationItemSelectedListener(this)
    }

    // Returns today's date in format: YYYY/MM/DD
    private fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("yyyy/MM/dd",java.util.Locale.getDefault())
        val result = sdf.format(Date())
        Log.d(TAG, "[getCurrentDate]: current date is $result")
        return result
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(TAG, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            // Make location information available
            enableLocation()
            // Warn user if wallet is full
            if (walletList.size == MAX_COINS_LIMIT) {
                Toast.makeText(this,"Can't collect coins, clean up your wallet!", Toast.LENGTH_SHORT).show()
            }
            // Get current date
            val currDate = getCurrentDate()
            if (currDate.equals(downloadDate)) {
                // Only for testing purposes - reset progress
//                numDayCollectedCoins = 0
//                visitedMarkerIdList = mutableSetOf()
//                walletList = ArrayList()
//                bankBalance = 0
//                bankAccount = ArrayList()

                // Already played today, render markers directly
                Log.d(TAG,"[onMapReady] Already played today, rendering markers directly")
                renderJson(map,mapJson)
            } else {
                // First time playing today
                // Reset values and download coin map
                Log.d(TAG,"[onMapReady] First time playing today, reset values and download map from server")
                downloadDate = currDate
                numDayCollectedCoins = 0
                visitedMarkerIdList = mutableSetOf()
                val downloadUrl = "http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson"
                Log.d(TAG,"[onMapReady] Downloading from $downloadUrl")
                val downloadFileTask = DownloadFileTask(this)
                downloadFileTask.execute(downloadUrl)
            }
        }
    }

    private fun renderJson(map:MapboxMap?, geoJson: JSONObject) {
        if (map == null) {
            Log.d(TAG, "[renderJson] map is null")
        }
        else {
            val coins = geoJson.getJSONArray("features")
            for (i in 0 until coins.length()) {
                val coin = coins.getJSONObject(i)
                val coinProps = coin.getJSONObject("properties")
                val coinId = coinProps.getString("id")
                if (visitedMarkerIdList.contains(coinId)) {
                    Log.d(TAG,"[renderJson] Marker already visited today, not rendering it")
                    continue
                }
                val coinCurrency = coinProps.getString("currency")
                val coinValue = coinProps.getString("value").toDouble()
                val coinCoords = coin.getJSONObject("geometry").getJSONArray("coordinates")
                val coinLatLng = LatLng(coinCoords.get(1) as Double,coinCoords.get(0) as Double)
                val coinColor = Color.parseColor(coinProps.getString("marker-color"))
                // Build custom marker icon - colour only
                val iconFactory = IconFactory.getInstance(this)
                val iconBitmap = getBitmapFromVectorDrawable(this, R.drawable.ic_place_24dp)
                val iconColorBitmap = tintImage(iconBitmap,coinColor)
                val icon = iconFactory.fromBitmap(iconColorBitmap)
                // Build marker
                val markerOpts = MarkerOptions().title(String.format("%.3f",coinValue)).snippet(coinCurrency).icon(icon).position(coinLatLng)
                Log.d(TAG, "[renderJson] marker was added into the map and into markerList\n")
                val marker = map.addMarker(markerOpts)
                markerList.put(coinId,marker)
            }
        }
    }

    // Update currency exchange rates when new map downloaded
    private fun updateExchangeRates(geoJson: JSONObject) {
        val rates = geoJson.getJSONObject("rates")
        penyRate = rates.getDouble("PENY")
        dolrRate = rates.getDouble("DOLR")
        quidRate = rates.getDouble("QUID")
        shilRate = rates.getDouble("SHIL")
        Log.d(TAG,"[getExchangeRates] Updated rate for PENY is " + String.format("%.2f",penyRate))
        Log.d(TAG,"[getExchangeRates] Updated rate for DOLR is " + String.format("%.2f",dolrRate))
        Log.d(TAG,"[getExchangeRates] Updated rate for QUID is " + String.format("%.2f",quidRate))
        Log.d(TAG,"[getExchangeRates] Updated rate for SHIL is " + String.format("%.2f",shilRate))
    }

    // Gets bitmap from a vector drawable
    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // Colours bitmap with a specified oolour
    private fun tintImage(bitmap: Bitmap, color: Int): Bitmap {
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        val bitmapResult = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapResult)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bitmapResult
    }

    // Showing user location
    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(TAG, "Permissions are not granted")
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
            Log.d(TAG, "mapView is null")
        } else {
            if (map == null) {
                Log.d(TAG, "map is null")
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
        Log.d(TAG, "[setCameraPosition] Current position: Lat: ${location.latitude} Lng: ${location.longitude}")
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    // When location changes,
    // If user is sufficiently close to any coin (at least 25m), remove it from the map
    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(TAG, "[onLocationChanged] location is null")
        // No need to check distances if wallet is false
        } else if (walletList.size == MAX_COINS_LIMIT) {
            return
        } else {
            originLocation = location
            setCameraPosition(originLocation)
            // Compute distance to markers to each marker, checking if any is sufficiently close
            val mapIt = markerList.entries.iterator()
            while (mapIt.hasNext()) {
                val pair = mapIt.next()
                val markerId = pair.key
                val marker = pair.value
                val distToMarker = distanceToMarker(originLocation,marker)
                // If user sufficiently close, remove marker from map, add it to user's wallet and notify user
                if (distToMarker <= MAX_MARKER_DISTANCE) {
                    Log.d(TAG,"[onLocationChanged] Close to marker $markerId")
                    mapIt.remove()
                    map!!.removeMarker(marker)
                    Toast.makeText(this,"Coin ${marker.snippet} with value ${marker.title} collected", Toast.LENGTH_SHORT).show()
                    visitedMarkerIdList.add(markerId)
                    val coin = Coin(markerId,marker.snippet, marker.title.toDouble(),false)
                    walletList.add(coin)
                    numDayCollectedCoins++
                }
                if (numDayCollectedCoins == MAX_DAILY_COINS) {
                    openCollectAllCoinsDialog()
                }
            }
        }
    }
    // Computes distance between the user and a marker
    private fun distanceToMarker(location: Location, marker : Marker) : Float {
        val locationMarker = Location("locationMarker")
        locationMarker.latitude = marker.position.latitude
        locationMarker .longitude = marker.position.longitude
        return location.distanceTo(locationMarker)
    }

    // Opening information dialog when user collects all coins on the day
    private fun openCollectAllCoinsDialog() {
        Log.d(TAG, "[openCollectAllCOinsDialog] Opening information dialog for collecting all coins")
        val allCoinsDialog = AlertDialog.Builder(this)
        allCoinsDialog.setTitle("Collected all coins!")
        // To be done: Change daily bonus value appropriately
        allCoinsDialog.setMessage("Congratulations, you have successfully collected all coins today!" +
         " You'll receive 100 GOLD into your bank account as a reward."
                + " Play again tomorrow to get another daily bonus.")
        val dailyBonusTransfer = BankTransfer(getCurrentDate(),"Received daily bonus of 100 GOLD",100.0, bankBalance + 100.0)
        bankBalance += 100
        bankAccount.add(dailyBonusTransfer)
        Log.d(TAG, "[openCollectAllCOinsDialog] Daily bonus added to bank account")
        // Clicking OK dismisses the dialog
        allCoinsDialog.setPositiveButton("OK") { _: DialogInterface?, _: Int -> }
        allCoinsDialog.show()
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(TAG, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain:
                                     MutableList<String>?) {
        Log.d(TAG, "Permissions: $permissionsToExplain")
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(TAG, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            Toast.makeText(this,"Review location permission settings", Toast.LENGTH_SHORT).show()
        }
    }

    public override fun onStart() {
        super.onStart()
        mapView?.onStart()
         // Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // Recall map variables
        downloadDate = settings.getString("lastDownloadDate", "")
        mapString = settings.getString("lastCoinMap","")
        if (mapString.equals("")) {
            mapJson = JSONObject()
        }
        else {
            mapJson = JSONObject(mapString)
        }
        numDayCollectedCoins = settings.getString("numDayCollectedCoins","0").toInt()
        visitedMarkerIdList = settings.getStringSet("visitedMarkers", mutableSetOf())
        Log.d(TAG, "[onStart] Recalled lastDownloadDate is $downloadDate")
        Log.d(TAG, "[onStart] Recalled lastCoinMap is $mapString")
        Log.d(TAG, "[onStart] Recalled numDayCollectedCoins is $numDayCollectedCoins")
        Log.d(TAG, "[onStart] Recalled visited markers")
        // Recall exchange rates
        penyRate = settings.getString("penyRate","0.0").toDouble()
        dolrRate = settings.getString("dolrRate","0.0").toDouble()
        quidRate = settings.getString("quidRate","0.0").toDouble()
        shilRate = settings.getString("shilRate","0.0").toDouble()
        Log.d(TAG, "[onStop] Recalled PENY rate as $penyRate")
        Log.d(TAG, "[onStop] Recalled DOLR rate as $dolrRate")
        Log.d(TAG, "[onStop] Recalled QUID rate as $quidRate")
        Log.d(TAG, "[onStop] Recalled SHIL rate as $shilRate")
        loadWallet() // Load wallet from Firestore
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
        mapString = mapJson.toString()
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        // Store map values in Shared Preferences
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("lastCoinMap",mapString)
        editor.putString("numDayCollectedCoins", numDayCollectedCoins.toString())
        editor.putStringSet("visitedMarkers", visitedMarkerIdList)
        Log.d(TAG, "[onStop] Stored lastDownloadDate of $downloadDate")
        Log.d(TAG, "[onStop] Stored lastCoinMap as $mapString")
        Log.d(TAG, "[onStop] Stored number of collected coins as $numDayCollectedCoins")
        Log.d(TAG, "[onStop] Stored visited markers")
        // Store exchange rates in Shared Preferences
        editor.putString("penyRate", penyRate.toString())
        editor.putString("dolrRate", dolrRate.toString())
        editor.putString("quidRate", quidRate.toString())
        editor.putString("shilRate", shilRate.toString())
        Log.d(TAG,"[onStop] stored rate of PENY as $penyRate")
        Log.d(TAG, "[onStop] stored rate of DOLR as $dolrRate")
        Log.d(TAG, "[onStop] stored rate of QUID as $quidRate")
        Log.d(TAG, "[onStop] stored rate of SHIL as $shilRate")
        editor.apply()
    }

    // Store user's wallet in Firestore
    private fun saveWallet() {
        val gson = Gson()
        val json = gson.toJson(walletList)
        db.collection(COLLECTION_KEY).document(uid).update(WALLET_KEY,json)
        Log.d(TAG, "[saveWallet] Stored wallet state as $json")
    }

    // Load wallet from Firestore
    private fun loadWallet() {
        val gson = Gson()
        // Find JSON respresentation of user's wallet in FireStore
        val userDocRef = db.collection(COLLECTION_KEY).document(uid)
        userDocRef.get().addOnCompleteListener{ task : Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val walletString = task.result!!.get(WALLET_KEY).toString()
                Log.d(TAG,"[loadWallet] JSON representation of wallet: $walletString")
                if (walletString.equals("[]")) {
                    walletList = ArrayList()
                } else {
                    val type = object : TypeToken<ArrayList<Coin>>(){}.type
                    walletList = gson.fromJson(walletString, type)
                }

            } else {
                Log.d(TAG,"[loadWallet] Failed to extract JSON representation of wallet state")
            }
        }
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

    // Render markers and extract exchange rates after coin map is downloaded
    override fun downloadComplete(result: JSONObject) {
        mapJson = result
        if (mapJson.length() != 0) {
            Log.d(TAG, "[downloadComplete] successfully extracted map as JSON object")
            // Render markers after download was completed
            renderJson(map, mapJson)
            updateExchangeRates(mapJson)
        }
        else {
            Log.d(TAG, "[downloadComplete] couldn't extract map")
        }
    }

    // Open Navigation drawer when menu item clicked
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle navigation drawer click events
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mDrawerLayout.closeDrawers() // Close drawer
        when (item.itemId) {
            //Signs out current user upon confirmation and returns him to Log in screen
            R.id.sign_out -> {
                // Confirmation dialog for user to confirm this action
                val confirmSignOut = AlertDialog.Builder(this)
                confirmSignOut.setTitle("Confirm sign out")
                confirmSignOut.setMessage("Are you sure that you want to sign out from the game?")
                confirmSignOut.setCancelable(false)
                confirmSignOut.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                    // If user confirms action
                    // Save wallet into FireStore and sign out
                    saveWallet()
                    Log.d(TAG,"[onNavigationItemSelected] Signing out user $uid")
                    mAuth.signOut()
//                    val resetIntent = Intent(this, LoginActivity::class.java)
//                    resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    startActivity(resetIntent)
                    Toast.makeText(this,"Successfully signed out", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this,LoginActivity::class.java))
                    finish()

                }
                // Otherwise nothing happens
                confirmSignOut.setNegativeButton("No") { _: DialogInterface?, _: Int -> }
                confirmSignOut.show()
            }
            // Saves wallet and starts Wallet screen
            R.id.wallet -> {
                saveWallet()
                startActivity(Intent(this,WalletActivity::class.java))
            }
            // Saves wallet and starts Bank account screen
            R.id.bank_account -> {
                saveWallet()
                startActivity(Intent(this,BankActivity::class.java))
            }
        }
        return true
    }
}