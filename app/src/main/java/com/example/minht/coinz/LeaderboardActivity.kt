package com.example.minht.coinz

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlin.collections.ArrayList

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var numDisplayItems = DEFAULT_DISPLAY_NUM // How many items will be displayed, [X] by default

    private var leaderboard : ArrayList<LeaderboardItem> = ArrayList() // Current leaderboard
    private lateinit var scoreTextView : TextView
    private lateinit var scoreListView : ListView
    private lateinit var scoreAdapter : LeaderboardItemAdapter

    // User's stats
    private var userRank =-1
    private var userName = ""
    private var userScore = 0.0
    private lateinit var userRankTextView: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var userScoreTextView: TextView

    companion object {
        const val PREFS_FILE = "MyPrefsFile"
        const val TAG = "LeaderboardActivity" // LOgging purposes
        const val DEFAULT_DISPLAY_NUM = 100 // How many top players are displayed
        // Keys in Intent/Firestore
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY= "Username"
        const val SCORE_KEY = "Score"
        // Keys for Shared Preferences
        const val NUM_PLAYERS_KEY = "numPlayers"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        scoreTextView = findViewById(R.id.scoresTextView)
        scoreListView = findViewById(R.id.scores)
        userRankTextView = findViewById(R.id.playerRank)
        userNameTextView = findViewById(R.id.playerUsername)
        userScoreTextView = findViewById(R.id.playerScore)
    }

    override fun onStart() {
        super.onStart()
        // Get data passed from map
        val bundle = intent.extras
        userName= bundle.getString(USERNAME_KEY)
        userScore = bundle.getDouble(SCORE_KEY)
        userNameTextView.text = userName
        userScoreTextView.text = String.format("%.2f",userScore)
        Log.d(TAG, "[onStart] Username of current user: $userName")
        Log.d(TAG, "[onStart] Score of current user: $userScore")
        cleanUp()
        // Generate leaderboard as usual if network connection available, otherwise sign out immediately
        if (isNetworkAvailable()) {
            Log.d(TAG, "[onStart] User connected, start as usual")
            generateLeaderboard()
        }
        else {
            Log.d(TAG, "[onStart] User disconnected, sign out")
            signOut()
            Toast.makeText(this,"Can't communicate with server. Check your internet " +
                    "connection and log in again.", Toast.LENGTH_SHORT).show()
        }
    }

    // Generate current leaderboard
    // Read Firestore, order by score field
    private fun generateLeaderboard() {
        val settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val numPlayers = settings.getInt(NUM_PLAYERS_KEY,1) // Extract total number of players
        Log.d(TAG,"[generateLeaderboard] There are $numPlayers players")
        val usersRef = db.collection(COLLECTION_KEY)
        // If less players than default display number, only display that number of players
        if (numPlayers < numDisplayItems) {
            numDisplayItems = numPlayers
            Log.d(TAG,"[generateLeaderboard] Less players than default number of items" +
                    " to be displayed. Only displaying $numDisplayItems items.")
        }
        scoreTextView.text = "Top $numDisplayItems players:"
        // Get documents, sorted by score field in descending order
        usersRef.orderBy(SCORE_KEY, Query.Direction.DESCENDING).limit(numDisplayItems.toLong())
                .get().addOnCompleteListener{task : Task<QuerySnapshot> ->
                    if (task.isSuccessful) {
                        val docs = task.result!!.documents
                        var rank = 1 // Keep track of current rank
                        for (doc in docs) {
                            val username = doc.getString(USERNAME_KEY)
                            val score = doc.getDouble(SCORE_KEY)
                            if (username == null || score == null) {
                                Log.d(TAG,"[generateLeaderboard] Document malformed")
                                Toast.makeText(this,"Error loading data", Toast.LENGTH_SHORT).show()
                                continue
                            }
                            else {
                                var leaderBoardItem:LeaderboardItem?
                                // If current user found, we know his rank
                                // Use it to fill user's info
                                if (username == userName) {
                                    Log.d(TAG,"[generateLeaderboard] Current user found")
                                    leaderBoardItem = LeaderboardItem(rank,username,score, true)
                                    userRank = rank
                                }
                                else {
                                    leaderBoardItem = LeaderboardItem(rank,username,score, false)
                                }
                                leaderboard.add(leaderBoardItem)
                                rank++
                                Log.d(TAG,"[generateLeaderboard] Item [$leaderBoardItem] added")
                            }
                        }
                        scoreAdapter = LeaderboardItemAdapter(this,leaderboard)
                        scoreListView.adapter=scoreAdapter
                        // Find rank of player
                        if (userRank == -1) {
                            // If player not in top players, need to find how many players have a higher score
                            Log.d(TAG,"[generateLeaderboard] Player not in top, need more to find rank")
                            findUserRank()
                        }
                        else {
                            // If user in top players, we already have his rank
                            Log.d(TAG,"[generateLeaderboard] Player in top, use already obtained rank")
                            userRankTextView.text = userRank.toString()
                        }
                    }
                    else {
                        Log.d(TAG,"[generateLeaderboard] Problem fetching data")
                        val message = task.exception!!.message
                        Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    // Find rank of current user, if user not in top players
    private fun findUserRank() {
        val usersRef = db.collection(COLLECTION_KEY)
        usersRef.whereGreaterThan(SCORE_KEY,userScore).get().addOnCompleteListener{task : Task<QuerySnapshot> ->
            if (task.isSuccessful) {
                // Compute number of documents where score is greater than user's score
                // Rank is this number + 1
                val rank = task.result!!.documents.size + 1
                userRankTextView.text = rank.toString()
                Log.d(TAG,"[findUserRank] User's rank is $rank")
            }
            else {
                Log.d(TAG,"[findUserRank] Problem fetching data")
                val message = task.exception!!.message
                Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                userRankTextView.text = "-"
            }
        }
    }

    // Clean up resources from previous session
    private fun cleanUp() {
        leaderboard.clear()
        numDisplayItems = DEFAULT_DISPLAY_NUM
        userRank = -1
    }

    // Check if internet connection is available
    private fun isNetworkAvailable() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    // Sign out user if no network connection available when activity launched
    private fun signOut() {
        Log.d(TAG,"[signOut] Signing out user $userName")
        mAuth.signOut()
        val resetIntent = Intent(this, LoginActivity::class.java)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(resetIntent)
    }
}