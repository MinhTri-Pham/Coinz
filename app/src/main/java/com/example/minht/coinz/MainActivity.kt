package com.example.minht.coinz

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
//import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import kotlinx.android.synthetic.main.activity_main.*
//import com.mapbox.mapboxsdk.annotations.IconFactory
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
    private var originLocation : Location? = null
    private lateinit var permissionsManager: PermissionsManager
    private var locationEngine : LocationEngine? = null
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    // Coin collection mechanism variables
    private var numCollectCoins = 0 // Number of coins collected on the current day
    private var markerList = HashMap<String,Marker>() // HashMap of markers shown in the map
    private var visitedMarkerSet : MutableSet<String> = mutableSetOf() // Set of markers already visited by user on the day
    private var fullWallet = false
    private var collectionBonus = 0
    private var collectionBonusReceived = false // Whether player received daily bonus already
    private var connectionFlag = false // Whether connection warning issued user

    // Map downloading
    private var latestFlag = false
    private var downloadDate = "" // Format: YYYY/MM/DD
    private var mapString = "" // String with map data
    private lateinit var mapJson : JSONObject // JSON object of map

    // Firebase/Firestore variables
    private lateinit var mAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var db : FirebaseFirestore

    // Wallet & Bank Account
    private var walletList : ArrayList<Coin> = ArrayList()  // List of coins in user's wallet
    private var bankAccount : BankAccount? = null
    private var numDepositCoins = 0

    // Exchange rates for currencies
    private var penyRate = 0.0
    private var dolrRate = 0.0
    private var quidRate = 0.0
    private var shilRate = 0.0

    // Other user info
    private var userName = ""
    private var userScore = 0.0
    private var userLastPlay = ""
    private var userDist = 0.0
    private var userCals = 0.0
    private var userMapsCompleted = 0

    // Display variables
    private lateinit var mDrawerLayout : DrawerLayout
    private lateinit var mapDate : TextView
    private lateinit var progressInfo : TextView
    private lateinit var bonusInfo : TextView
    private lateinit var penyInfo : TextView
    private lateinit var dolrInfo : TextView
    private lateinit var quidInfo : TextView
    private lateinit var shilInfo : TextView

    // Constants
    companion object {
        const val PREFS_FILE = "MyPrefsFile" // Storing data
        const val TAG = "MainActivity" // Logging purposes
        // Keys to access values in Firestore
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY = "Username"
        const val EMAIL_KEY = "Email"
        const val WALLET_KEY = "Wallet"
        const val BANK_KEY = "Bank"
        const val NUM_DEPOSIT_KEY = "Number of deposited coins"
        const val SCORE_KEY = "Score"
        const val LAST_PLAY_KEY = "Last play date"
        const val VISITED_MARKERS_KEY = "Visited markers"
        const val NUM_COINS_KEY = "Number of collected coins"
        const val DIST_KEY = "Distance walked"
        const val CAL_KEY = "Calories burned"
        const val NUM_MAP_KEY = "Number of completed maps"
        const val DAILY_BONUS_KEY = "Daily bonus"
        // Keys for Shared Preferences
        const val DOWNLOAD_DATE_KEY = "lastDownloadDate" // Date of map downloaded last
        const val MAP_KEY = "lastCoinMap" // Latest coin map
        // Other constants
        const val MAX_MARKER_DISTANCE = 25 // Maximum distance from coin to collect it
        const val MAX_DAILY_COINS = 50 // Maximum number of coins that can be collected per day
        const val MAX_COINS_LIMIT = 200 // Maximum number of coins that can be in the wallet at any time
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
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        db.firestoreSettings = settings
        setUpNavDrawer()
        Mapbox.getInstance(this, getString(R.string.access_token))
        // Map info views
        mapDate = findViewById(R.id.mapDate)
        progressInfo = findViewById(R.id.progressInfo)
        bonusInfo = findViewById(R.id.bonusInfo)
        penyInfo = findViewById(R.id.penyInfo)
        dolrInfo = findViewById(R.id.dolrInfo)
        quidInfo = findViewById(R.id.quidInfo)
        shilInfo = findViewById(R.id.shilInfo)
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
//        userRef.get().addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
//            if (documentSnapshot!!.exists()) {
//                userName = documentSnapshot.getString(USERNAME_KEY)!!
//                val email = documentSnapshot.getString(EMAIL_KEY)
//                val usernameText = "Welcome back $userName!"
//                navUsernameText.text = usernameText
//                navEmailText.text = email
//                Log.d(TAG,"[onCreate] Created welcome message")
//            } else {
//                Log.d(TAG,"[onCreate] User document not found")
//            }
//        }
        userRef.get().addOnCompleteListener {task: Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                userName = task.result!!.getString(USERNAME_KEY)!!
                val email = task.result!!.getString(EMAIL_KEY)
                val usernameText = "Welcome back $userName!"
                navUsernameText.text = usernameText
                navEmailText.text = email
                Log.d(TAG,"[onCreate] Created welcome message")
            }
            else {
                val message = task.exception!!.message
                Log.d(TAG, "Error getting data")
                Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
            }
        }

        navigationView.setNavigationItemSelectedListener(this)
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
            val currDate = getCurrentDate()
            // Handle values that change daily before rendering markers
            // That is number of collected coins and set of visited markers
            val gson = Gson()
            val userDocRef = db.collection(COLLECTION_KEY).document(uid)
            userDocRef.get().addOnCompleteListener { task: Task<DocumentSnapshot> ->
                if (task.isSuccessful) {
                    val taskResult = task.result
                    if (taskResult!!.exists()) {
                        // Load latest date when the user has played the game
                        userLastPlay = taskResult.getString(LAST_PLAY_KEY)!!
                        Log.d(TAG, "[onMapReady] Loaded last date when user played as $userLastPlay")
                        // If user played today, load daily variables from Firestore
                        if (userLastPlay == currDate) {
                            latestFlag = true
                            Log.d(TAG, "[onMapReady] User has played today, load as usual")
                            // Load set of visited markers
                            val dataString = taskResult.getString(VISITED_MARKERS_KEY)
                            Log.d(TAG, "[onMapReady] Loaded set of visited markers as: $dataString")
                            val type = object : TypeToken<MutableSet<String>>() {}.type
                            visitedMarkerSet = gson.fromJson(dataString, type)
                            // Load number of collected coins
                            numCollectCoins = taskResult.getLong(NUM_COINS_KEY)!!.toInt()
                            Log.d(TAG, "[onMapReady] Loaded number of collected coins as $numCollectCoins")
                            numDepositCoins = taskResult.getLong(NUM_DEPOSIT_KEY)!!.toInt()
                            Log.d(TAG, "[onMapReady] Loaded number of deposited coins as $numDepositCoins")
                            progressInfo.text = "Coins: $numCollectCoins / $MAX_DAILY_COINS"
                            collectionBonusReceived = taskResult.getBoolean(DAILY_BONUS_KEY)!!
                            // Goes wrong
//                            updateDailyValues(userDocRef,currDate)
                            // Goes wrong
                        }
                        // Otherwise, reset daily variables and update last play date to today
                        else {
                            Log.d(TAG,"[onMapReady] First time playing today, resetting values")
                            updateDailyValues(userDocRef,currDate)
                        }
                        // Download map if necessary
                        if (currDate == downloadDate) {
//                          Today's map has been downloaded and stored in Shared Preferences, render markers directly
                            Log.d(TAG,"[onMapReady] Map has already been downloaded today, rendering markers directly")
                            renderJson(map,mapJson)
                            mapDate.text = "Map date: $currDate"
                            penyInfo.text = "PENY: ${String.format("%.2f",penyRate)}"
                            dolrInfo.text = "DOLR: ${String.format("%.2f",dolrRate)}"
                            quidInfo.text = "QUID: ${String.format("%.2f",quidRate)}"
                            shilInfo.text = "SHIL: ${String.format("%.2f",shilRate)}"

                        } else {
                            // First time today's map is used, need to download it
                            Log.d(TAG,"[onMapReady] First time this map was used today, download map from server")
                            downloadMap(currDate)
                        }
                    }
                }
                else {
                    Log.d(TAG,"[onMapReady] Problem with fetching data")
                    val message = task.exception!!.message
                    Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadMap(date : String) {
        val downloadUrl = "http://homepages.inf.ed.ac.uk/stg/coinz/$date/coinzmap.geojson"
        Log.d(TAG,"[onMapReady] Downloading from $downloadUrl")
        val downloadFileTask = DownloadFileTask(this)
        downloadFileTask.execute(downloadUrl)
    }

    private fun updateDailyValues(docRef : DocumentReference, currDate: String) {
        val gson = Gson()
        userLastPlay = currDate
        visitedMarkerSet = mutableSetOf()
        numCollectCoins = 0
        progressInfo.text = "Coins: 0 / $MAX_DAILY_COINS"
        numDepositCoins = 0
        collectionBonusReceived = false
        docRef.update(LAST_PLAY_KEY, userLastPlay)
        docRef.update(VISITED_MARKERS_KEY, gson.toJson(visitedMarkerSet))
        docRef.update(NUM_COINS_KEY,0)
        docRef.update(NUM_DEPOSIT_KEY,0)
        docRef.update(DAILY_BONUS_KEY,false)
    }

    // Render the coin map
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
                if (visitedMarkerSet.contains(coinId)) {
                    Log.d(TAG,"[renderJson] Marker $coinId already visited today, not rendering it")
                    continue
                }
                val coinCurrency = coinProps.getString("currency")
                val coinValue = coinProps.getString("value").toDouble()
                val coinCoords = coin.getJSONObject("geometry").getJSONArray("coordinates")
                val coinLatLng = LatLng(coinCoords.get(1) as Double,coinCoords.get(0) as Double)
                // Build custom marker icon - colour only
                // Disabled for now, see below comment
                // BUG: Disappear randomly when map changes (zoom in/out, removing one)
                val iconFactory = IconFactory.getInstance(this)
                var icon : Icon? = null
                when (coinCurrency) {
                    "PENY"-> {
                        icon = iconFactory.fromResource(R.drawable.red)
                    }
                    "DOLR"-> {
                        icon = iconFactory.fromResource(R.drawable.green)
                    }
                    "QUID"-> {
                        icon = iconFactory.fromResource(R.drawable.yellow)
                    }
                    "SHIL"-> {
                        icon = iconFactory.fromResource(R.drawable.blue)
                    }
                }
                val markerOpts = MarkerOptions().title(String.format("%.3f",coinValue)).snippet(coinCurrency).icon(icon).position(coinLatLng)
                val marker = map.addMarker(markerOpts)
                markerList[coinId] = marker
                Log.d(TAG, "[renderJson] Marker $coinId was added into the map and into markerList\n")
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
        Log.d(TAG,"[updateExchangeRates] Updated rate for PENY is " + String.format("%.3f",penyRate))
        Log.d(TAG,"[updateExchangeRates] Updated rate for DOLR is " + String.format("%.3f",dolrRate))
        Log.d(TAG,"[updateExchangeRates] Updated rate for QUID is " + String.format("%.3f",quidRate))
        Log.d(TAG,"[updateExchangeRates] Updated rate for SHIL is " + String.format("%.3f",shilRate))
        val settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("penyRate", penyRate.toString())
        editor.putString("dolrRate", dolrRate.toString())
        editor.putString("quidRate", quidRate.toString())
        editor.putString("shilRate", shilRate.toString())
        editor.apply()
        penyInfo.text = "PENY: ${String.format("%.3f",penyRate)}"
        dolrInfo.text = "DOLR: ${String.format("%.3f",dolrRate)}"
        quidInfo.text = "QUID: ${String.format("%.3f",quidRate)}"
        shilInfo.text = "SHIL: ${String.format("%.3f",shilRate)}"
    }

    private fun getCollectionBonus() {
        collectionBonus = when {
            userMapsCompleted < 10 -> 100
            userMapsCompleted < 50 -> 400
            userMapsCompleted < 100 -> 700
            userMapsCompleted < 250 -> 1200
            userMapsCompleted < 500 -> 2000
            userMapsCompleted < 1000 -> 3500
            else -> 6000
        }
        bonusInfo.text = "Current bonus: $collectionBonus GOLD"
    }

    // Returns today's date in format: YYYY/MM/DD
    private fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("yyyy/MM/dd",java.util.Locale.getDefault())
        val result = sdf.format(Date())
        Log.d(TAG, "[getCurrentDate]: current date is $result")
        return result
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
        locationEngine!!.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine!!.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine!!.addLocationEngineListener(this)
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
                val lifecycle = lifecycle
                lifecycle.addObserver(locationLayerPlugin)
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        Log.d(TAG, "[setCameraPosition] Current position: Lat: ${location.latitude} Lng: ${location.longitude}")
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    // When location changes
    // If user is sufficiently close to any coin (at least 25m), remove it from the map
    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(TAG, "[onLocationChanged] Location is null")
        } else {
            if (originLocation != null) {
                val distanceUser = originLocation!!.distanceTo(location)
                userDist += distanceUser
                userCals += distanceUser / 1000 * 65 // Only estimate 70 cal/km
            }
            originLocation = location
            setCameraPosition(originLocation!!)
            Log.d(TAG,"[onLocationChanged] Distance walked: $userDist")
            Log.d(TAG,"[onLocationChanged] Calories burned: $userCals")
            // Check if map expired
            val currDay = getCurrentDate()
            if ((currDay != userLastPlay && userLastPlay != "" && latestFlag)) {
                Log.d(TAG,"[onLocationChanged] Map expired")
                latestFlag = false
                signOut()
                Toast.makeText(this, "Today's map has expired. Log in again to download " +
                        "the new map", Toast.LENGTH_SHORT).show()
            }
            // Check internet connection
            if (!isNetworkAvailable() && !connectionFlag) {
                Log.d(TAG,"[onLocationChanged] User disconnected, disable app")
                Toast.makeText(this, "No connection found, collection is disabled. Check your internet collection.",
                        Toast.LENGTH_SHORT).show()
                connectionFlag = true
            }
            // If user reconnects, collection is enabled
            if (isNetworkAvailable() && connectionFlag) {
                Log.d(TAG, "[onLocationChanged] User reconnected, enable app")
                connectionFlag = false
            }
            // Block further notifications and collection if full wallet/map completed/not connected
            // And warning was already issued
            if (fullWallet || collectionBonusReceived || connectionFlag) {
                Log.d(TAG, "[onLocationChanged] Can't collect anymore, since wallet full/map completed")
                return
            }
            val mapIt = markerList.entries.iterator()
            while (mapIt.hasNext()) {
                val pair = mapIt.next()
                val markerId = pair.key
                val marker = pair.value
                val distToMarker = distanceToMarker(originLocation!!,marker)
                // If user sufficiently close, remove marker from map, add it to user's wallet and notify user
                if (distToMarker <= MAX_MARKER_DISTANCE) {
                    map!!.removeMarker(marker)
                    Log.d(TAG, "[onLocationChanged] Coin $markerId of ${marker.snippet} with value ${marker.title} removed from map")
                    visitedMarkerSet.add(markerId)
                    val coin = Coin(markerId,marker.snippet, marker.title.toDouble(),false,true)
                    // If user already contains coin with same id, create a new unique id for it
                    // Important for actions with coins (transfer/deposit)
                    if (walletList.contains(coin)) {
                        Log.d(TAG,"[onLocationChanged] Duplicate coin $markerId found")
                        var isUnique = false
                        while (!isUnique) {
                            val uniqueId = UUID.randomUUID().toString()
                            coin.id = uniqueId
                            isUnique = !walletList.contains(coin)
                        }
                    }
                    walletList.add(coin)
                    numCollectCoins++
                    mapIt.remove()
                    progressInfo.text = "Coins: $numCollectCoins / $MAX_DAILY_COINS"
                    Log.d(TAG, "[onLocationChanged] Coin ${coin.id} of ${marker.snippet} with value ${marker.title} collected and added to wallet")
                    Toast.makeText(this,"Coin ${marker.snippet} with value ${marker.title} collected and added to wallet", Toast.LENGTH_SHORT).show()
                    if (numCollectCoins == MAX_DAILY_COINS && !collectionBonusReceived && bankAccount != null) {
                        collectionBonusReceived = true // Make sure player can't receive daily bonus more than once per day
                        userMapsCompleted++
                        // Create special dialogs for milestone completion
                        // Otherwise just dialog informing user about collection bonus
                        when (userMapsCompleted) {
                            10 -> {
                                collectionBonus = 400
                                Log.d(TAG, "[onLocationChanged] Collection bonus increased to " +
                                        "$collectionBonus")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("\nYou've completed your 10th map!\n" +
                                        "You now get a bigger reward of $collectionBonus coins.\n" +
                                        "Complete 50 maps to increase your bonus again.")
                                bonusInfo.show()
                            }
                            50 -> {
                                collectionBonus = 700
                                Log.d(TAG, "[onLocationChanged] Collection bonus increased to " +
                                        "$collectionBonus")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("\nYou've completed your 50th map!\n" +
                                        "You now get a bigger reward of $collectionBonus GOLD.\n" +
                                        "Complete 100 maps to increase your bonus again.")
                                bonusInfo.show()
                            }
                            100 -> {
                                collectionBonus = 1200
                                Log.d(TAG, "[onLocationChanged] Collection bonus increased to " +
                                        "$collectionBonus")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("\nYou've completed your 100th map!\n" +
                                        "You now get a bigger reward of $collectionBonus GOLD.\n" +
                                        "Complete 250 maps to increase your bonus again.")
                                bonusInfo.show()
                            }
                            250 -> {
                                collectionBonus = 2000
                                Log.d(TAG, "[onLocationChanged] Collection bonus increased to " +
                                        "$collectionBonus")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("\nYou've completed your 250th map!\n" +
                                        "You now get a bigger reward of $collectionBonus GOLD." +
                                        "\nComplete 500 maps to increase your bonus again. ")
                                bonusInfo.show()
                            }
                            500 -> {
                                collectionBonus = 3500
                                Log.d(TAG, "[onLocationChanged] Collection bonus increased to " +
                                        "$collectionBonus")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("\nYou've completed your 500th map!\n" +
                                        "You now get a bigger reward $collectionBonus GOLD.\n" +
                                        "Complete 1000 maps to increase your bonus again.")
                                bonusInfo.show()
                            }
                            1000 -> {
                                collectionBonus = 6000
                                Log.d(TAG, "[onLocationChanged] Collection bonus increased to " +
                                        "$collectionBonus")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("You've completed your 1000th map!\n" +
                                        "You now receive the maximum collection bonus $collectionBonus GOLD\n" +
                                        "on each completion of the map.")
                                bonusInfo.show()
                            }
                            else -> {
                                Log.d(TAG, "[onLocationChanged] No milestone, collection bonus " +
                                        "stays $collectionBonus GOLD")
                                val bonusInfo = AlertDialog.Builder(this,R.style.MyDialogTheme)
                                bonusInfo.setTitle("Collection bonus").setCancelable(true)
                                bonusInfo.setMessage("You've completed today's map! As a reward,\n" +
                                        "you receive a collection bonus $collectionBonus GOLD")
                                bonusInfo.show()
                            }
                        }
                        bonusInfo.text = "Current bonus: $collectionBonus GOLD"
                        bankAccount!!.bankTransfers.add(BankTransfer(getCurrentDate(),"Received collection bonus $collectionBonus GOLD", collectionBonus.toDouble(),bankAccount!!.balance + collectionBonus,ArrayList(),false))
                        bankAccount!!.balance += collectionBonus
                        userScore += collectionBonus
                        Log.d(TAG, "[onLocationChanged] Collection bonus added to bank account")
                }
                    // Warn user if wallet is full
                    if (walletList.size == MAX_COINS_LIMIT) {
                        fullWallet = true
                        Log.d(TAG, "[onLocationChanged] Wallet became full. Won't be able to collect coins")
                        Toast.makeText(this,"Can't collect coins anymore, clean up your wallet!", Toast.LENGTH_SHORT).show()
                    }
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

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(TAG, "[onConnected] requesting location updates")
        locationEngine!!.requestLocationUpdates()
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
        // Handle location engine
        if (locationEngine != null) {
            try {
                locationEngine!!.requestLocationUpdates()
            } catch (ignored: SecurityException) {}
            locationEngine!!.addLocationEngineListener(this)
        }
        // Check internet connection
        if (isNetworkAvailable()) {
            Log.d(TAG,"[onStart] User connected, start as usual")
            Toast.makeText(this,"Please wait while content updates", Toast.LENGTH_SHORT).show()
            // Restore data from Shared Preferences
            val settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            // Recall map variables
            downloadDate = settings.getString(DOWNLOAD_DATE_KEY, "")
            mapString = settings.getString(MAP_KEY,"")
            mapJson = if (mapString == "") {
                JSONObject()
            } else {
                JSONObject(mapString)
            }
            Log.d(TAG, "[onStart] Recalled lastDownloadDate is $downloadDate")
            Log.d(TAG, "[onStart] Recalled lastCoinMap is $mapString")
            Log.d(TAG, "[onStart] Recalled number of collected coins is $numCollectCoins")
            Log.d(TAG, "[onStart] Recalled number of visited markers today as ${visitedMarkerSet.size}\")")
            // Recall exchange rates
            penyRate = settings.getString("penyRate","0.0").toDouble()
            dolrRate = settings.getString("dolrRate","0.0").toDouble()
            quidRate = settings.getString("quidRate","0.0").toDouble()
            shilRate = settings.getString("shilRate","0.0").toDouble()
            Log.d(TAG, "[onStart] Recalled PENY rate as $penyRate")
            Log.d(TAG, "[onStart] Recalled DOLR rate as $dolrRate")
            Log.d(TAG, "[onStart] Recalled QUID rate as $quidRate")
            Log.d(TAG, "[onStart] Recalled SHIL rate as $shilRate")
            loadData() // Load data from Firestore
        }
        else {
            Log.d(TAG, "[onStart] User disconnected")
            forcedSignOut()
            Toast.makeText(this,"Can't communicate with server. Check your internet " +
                    "connection and log in again.", Toast.LENGTH_SHORT).show()
        }
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView?.onStop()
        // Handle location engine
        if (locationEngine != null) {
            locationEngine!!.removeLocationEngineListener(this)
            locationEngine!!.removeLocationUpdates()
        }
    }

    // Store user data in Firestore
    private fun saveData() {
        val gson = Gson()
        val userDoc = db.collection(COLLECTION_KEY).document(uid)
        // Save set of today's visited markers
        var dataString = gson.toJson(visitedMarkerSet)
        userDoc.update(VISITED_MARKERS_KEY,dataString)
        Log.d(TAG,"[saveData] Stored today's list of visited markers as $dataString")
        // Save number of today's number of collected coins
        userDoc.update(NUM_COINS_KEY,numCollectCoins)
        Log.d(TAG, "[saveData] Stored today's number of collected coins as $numCollectCoins")
        // Save number of today's number of deposited coins
        userDoc.update(NUM_DEPOSIT_KEY,numDepositCoins)
        Log.d(TAG, "[saveData] Stored today's number of deposited coins as $numDepositCoins")
        // Save wallet
        dataString = gson.toJson(walletList)
        userDoc.update(WALLET_KEY,dataString)
        Log.d(TAG, "[saveData] Stored wallet as $dataString")
        // Save bank account
        dataString = gson.toJson(bankAccount)
        userDoc.update(BANK_KEY,dataString)
        Log.d(TAG, "[saveData] Stored bank account as $dataString")
        // Save score
        userDoc.update(SCORE_KEY,userScore)
        Log.d(TAG, "[saveData] Stored score as $userScore")
        // Save distance walked
        userDoc.update(DIST_KEY, userDist)
        Log.d(TAG, "[saveData] Stored distance walked as $userDist")
        // Save calories burned
        userDoc.update(CAL_KEY, userCals)
        Log.d(TAG, "[saveData] Stored calories burned as $userCals")
        // Save number of maps completed burned
        userDoc.update(NUM_MAP_KEY, userMapsCompleted)
        Log.d(TAG, "[saveData] Stored number of completed maps as $userMapsCompleted")
        // Save whether user has received daily bonus
        userDoc.update(DAILY_BONUS_KEY,collectionBonusReceived)
        Log.d(TAG,"[saveData] Stored daily bonus receival as $collectionBonusReceived")
    }

    // Load persistent user data from Firestore
    // I.e. those that aren't updated daily, these are handled in onMapReady
    private fun loadData() {
        val gson = Gson()
        val userDocRef = db.collection(COLLECTION_KEY).document(uid)
        userDocRef.get().addOnCompleteListener{ task : Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val taskResult = task.result
                if (taskResult!!.exists()) {
                    // Load wallet
                    var dataString = taskResult.getString(WALLET_KEY)
                    Log.d(TAG,"[loadData] Loaded wallet as: $dataString")
                    val type = object : TypeToken<ArrayList<Coin>>(){}.type
                    walletList = gson.fromJson(dataString, type)
                    fullWallet = walletList.size >= MAX_COINS_LIMIT
                    if (fullWallet) {
                        Log.d(TAG, "[loadData] Wallet si full, can't collect coins.")
                        Toast.makeText(this, "Your wallet is full, you won't be able to collect coins! Clean up your wallet.",
                                Toast.LENGTH_SHORT).show()
                    }
                    // Load bank account
                    dataString = taskResult.getString(BANK_KEY)
                    Log.d(TAG,"[loadData] Loaded bank account as: $dataString")
                    bankAccount = gson.fromJson(dataString, BankAccount::class.java)
                    // Load score
                    userScore = taskResult.getDouble(SCORE_KEY)!!
                    Log.d(TAG,"[loadData] Loaded score as: $userScore")
                    // Load distance walked
                    userDist = taskResult.getDouble(DIST_KEY)!!
                    Log.d(TAG,"[loadData] Loaded distance walked as: $userDist")
                    // Load calories burned
                    userCals = taskResult.getDouble(CAL_KEY)!!
                    Log.d(TAG,"[loadData] Loaded calories burned as: $userCals")
                    // Load number of maps completed
                    userMapsCompleted = taskResult.getLong(NUM_MAP_KEY)!!.toInt()
                    Log.d(TAG,"[loadData] Loaded number of completed maps as: $userMapsCompleted")
                    getCollectionBonus()
                    // Goes wrong
//                    walletList = ArrayList()
//                    bankAccount = BankAccount(userName,0.0, ArrayList())
//                    userScore = 0.0
//                    userDist = 0.0
//                    userCals = 0.0
//                    userMapsCompleted = 0
//                    collectionBonus = 100
                    // Goes wrong
                }
                else {
                    Toast.makeText(this, "Problems with loading your data!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"[loadData] couldn't find document $uid")
                }
            } else {
                Log.d(TAG,"[loadData] Error when loading data")
                val message = task.exception!!.message
                Toast.makeText(this, "Error occurred when loading data: $message", Toast.LENGTH_SHORT).show()
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
            latestFlag = true
            downloadDate = getCurrentDate()
            mapDate.text = "Map date: $downloadDate"
            Log.d(TAG, "[downloadComplete] Successfully extracted map")
            val settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val editor = settings.edit()
            // Store map downloading values in Shared Preferences
            mapString = mapJson.toString()
            editor.putString(DOWNLOAD_DATE_KEY, downloadDate)
            editor.putString(MAP_KEY,mapString)
            Log.d(TAG, "[downloadComplete] Stored lastDownloadDate as $downloadDate")
            Log.d(TAG, "[downloadComplete] Stored lastCoinMap as $mapString")
            editor.apply()
            // Render markers after download was completed
            renderJson(map, mapJson)
            updateExchangeRates(mapJson)
        }
        else {
            Log.d(TAG, "[downloadComplete] Couldn't extract map")
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

    // Usual sign out
    private fun signOut() {
        saveData()
        Log.d(TAG,"[signOut] Signing out user $userName")
        mAuth.signOut()
        // Clear activity stack
        val resetIntent = Intent(this, LoginActivity::class.java)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(resetIntent)
    }

    // Forced sign out due to connectivity issues
    private fun forcedSignOut() {
        Log.d(TAG,"[signOut] Signing out user $userName")
        mAuth.signOut()
        val resetIntent = Intent(this, LoginActivity::class.java)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(resetIntent)
    }

    // Handle navigation drawer click events
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mDrawerLayout.closeDrawers() // Close drawer
        when (item.itemId) {
            //Signs out current user upon confirmation and returns him to Log in screen
            R.id.sign_out -> {
                // Confirmation dialog for user to confirm this action
                val confirmSignOut = AlertDialog.Builder(this,R.style.MyDialogTheme)
                confirmSignOut.setTitle("Confirm sign out")
                confirmSignOut.setMessage("Are you sure that you want to sign out from the game?")
                confirmSignOut.setCancelable(false)
                confirmSignOut.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                    signOut()
                    Toast.makeText(this,"Successfully signed out", Toast.LENGTH_SHORT).show()
                }
                // Otherwise nothing happens
                confirmSignOut.setNegativeButton("No") { _: DialogInterface?, _: Int -> }
                confirmSignOut.show()
            }
            // Saves wallet and starts Wallet screen
            R.id.wallet -> {
                saveData()
                startActivity(Intent(this,WalletActivity::class.java))
            }
            // Saves wallet and starts Bank account screen
            R.id.bank_account -> {
                saveData()
                startActivity(Intent(this,BankActivity::class.java))
            }
            R.id.gifts -> {
                saveData()
                startActivity(Intent(this, GiftActivity::class.java))
            }
            R.id.leaderboard -> {
                saveData()
                val leaderboardIntent = Intent(this,LeaderboardActivity::class.java)
                leaderboardIntent.putExtra(USERNAME_KEY,userName)
                leaderboardIntent.putExtra(SCORE_KEY,userScore)
                startActivity(leaderboardIntent)
            }
            R.id.stats -> {
                saveData()
                val statsIntent = Intent(this, StatsActivity::class.java)
                statsIntent.putExtra(DIST_KEY, userDist)
                statsIntent.putExtra(CAL_KEY, userCals)
                statsIntent.putExtra(NUM_MAP_KEY, userMapsCompleted)
                startActivity(statsIntent)
            }
        }
        return true
    }
    // Check if internet connection is available
    private fun isNetworkAvailable() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}