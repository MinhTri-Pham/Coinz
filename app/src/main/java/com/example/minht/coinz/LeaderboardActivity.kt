package com.example.minht.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlin.collections.ArrayList

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var numDisplayItems = DEFAULT_DISPLAY_NUM // How many items will be displayed, [X] by default

    private var leaderboard : ArrayList<LeaderboardItem> = ArrayList() // Current leaderboard
    private lateinit var scoreTextView : TextView
    private lateinit var scoreListView : ListView
    private lateinit var scoreAdapter : LeaderboardItemAdapter


    companion object {
        const val PREFS_FILE = "MyPrefsFile" // Storing data
        const val TAG = "LeaderboardActivity" // Debugging purposes
        const val DEFAULT_DISPLAY_NUM = 10
        // Keys in Firestore
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY= "Username"
        const val SCORE_KEY = "Score"
        // Keys for Shared Preferences
        const val NUM_PLAYERS_KEY = "numPlayers"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        db = FirebaseFirestore.getInstance()
        scoreTextView = findViewById(R.id.scoresTextView)
        scoreListView = findViewById(R.id.scores)
    }

    override fun onStart() {
        super.onStart()
        generateLeaderboard()
    }

    // Generate current leaderboard
    // Read Firestore, order by score field
    private fun generateLeaderboard() {
        leaderboard.clear() // Reset leaderboard
        numDisplayItems = DEFAULT_DISPLAY_NUM // Reset number of displayed items to default
        // First need total number of user of the app
        val settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val numPlayers = settings.getInt(NUM_PLAYERS_KEY,0)
        Log.d(TAG,"[generateLeaderboard] There are $numPlayers players")
        val usersRef = db.collection(COLLECTION_KEY)
        // If less users than default number, only display that number of items
        if (numPlayers < numDisplayItems) {
            numDisplayItems = numPlayers
            Log.d(TAG,"[generateLeaderboard] Less players than default number of items to be displayed. Only displaying $numDisplayItems items.")
        }
        scoreTextView.text = "Top $numDisplayItems players:"
        usersRef.orderBy(SCORE_KEY, Query.Direction.DESCENDING).limit(numDisplayItems.toLong())
                .get().addOnCompleteListener{task : Task<QuerySnapshot> ->
                    if (task.isSuccessful) {
                        val docs = task.result!!.documents // Documents of top players
                        var rank = 1
                        for (doc in docs) {
                            val username = doc.getString(USERNAME_KEY)
                            val score = doc.getDouble(SCORE_KEY)
                            if (username == null || score == null) {
                                Log.d(TAG,"[generateLeaderboard] Document malformed")
                                //Toast.makeText(this,"Error loading data", Toast.LENGTH_SHORT).show()
                                continue
                            }
                            else {
                                val leaderBoardItem = LeaderboardItem(rank,username,score)
                                leaderboard.add(leaderBoardItem)
                                rank++
                                Log.d(TAG,"[generateLeaderboard] Item [$leaderBoardItem] added")
                            }
                        }
                        scoreAdapter = LeaderboardItemAdapter(this,leaderboard)
                        scoreListView.adapter=scoreAdapter
                    }
                    else {
                        Log.d(TAG,"[generateLeaderboard] Problem fetching data")
                        Toast.makeText(this,"Failed to load data, check your internet connection", Toast.LENGTH_SHORT).show()
                    }
                }
    }
}