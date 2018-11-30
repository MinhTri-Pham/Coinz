package com.example.minht.coinz

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class GiftActivity : AppCompatActivity() {

    private var walletList : ArrayList<Coin> = ArrayList()
    private var giftList : ArrayList<Gift> = ArrayList()
    private lateinit var giftState : TextView
    private lateinit var giftListView : ListView
    private lateinit var giftAdapter: GiftAdapter

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        const val TAG = "GiftActivity" // Debugging purposes
        const val COLLECTION_KEY = "Users"
        const val WALLET_KEY = "Wallet"
        const val GIFTS_KEY = "Gifts"
        const val MAX_COINS_LIMIT = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gift)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        giftListView = findViewById(R.id.gift_list)
        giftState = findViewById(R.id.gift_state)
        giftListView.setOnItemClickListener { _, _, pos, _ ->
            if (isNetworkAvailable()) {
                val selectedGift = giftList[pos]
                // If no space in wallet, warn user
                if (walletList.size > MAX_COINS_LIMIT - selectedGift.contents.size) {
                    Toast.makeText(this, "Don't have enough space in your wallet! Clean up your wallet.",
                            Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "[onCreate] Can't open gift, since there's not enough space in the wallet")
                }
                else {
                    val giftPrompt = AlertDialog.Builder(this)
                    val giftMsg = "Confirm opening gift with contents:\n\n" + selectedGift.showContents()
                    giftPrompt.setTitle("Gift confirmation").setMessage(giftMsg).setCancelable(false)
                    giftPrompt.setPositiveButton("Open gift") { _: DialogInterface?, _: Int ->
                        openGift(selectedGift)
                    }
                    // If "Cancel" is pressed, dialog closes and no changes occur
                    giftPrompt.setNegativeButton("Cancel") { _, _ ->}
                    giftPrompt.show()
                }
            }
            else {
                Log.d(TAG,"[onStart] User disconnected, can't process gift")
                Toast.makeText(this,"Can't process contents of the gift! Check your internet" +
                        " connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Add contents of gifts to wallet and update screen
    private fun openGift(gift: Gift) {
        val coins = gift.contents
        giftList.remove(gift)
        // Make new unique ids for any duplicate coins
        for (coin in coins) {
            if (walletList.contains(coin)) {
                var isUnique = false
                while (!isUnique) {
                    val uniqueId = UUID.randomUUID().toString()
                    coin.id = uniqueId
                    isUnique = !walletList.contains(coin) // Check that new id is indeed indeed
                }
            }
            walletList.add(coin)
        }
        // Update screen
        generateSummary()
        giftAdapter = GiftAdapter(this,giftList)
        giftListView.adapter=giftAdapter
        Toast.makeText(this,"Gift opened!", Toast.LENGTH_SHORT).show()
        Log.d(TAG,"[openGift] Gift opened and contents added to wallet")
        // Save data
        saveData()
    }

    // Summary describing how many unopened gifts user has before listview
    private fun generateSummary() {
        val numGifts = giftList.size
        val boldStyle = StyleSpan(Typeface.BOLD)
        if (numGifts != 0) {
            val giftText = "Unopened gifts:"
            val giftMsg = "Unopened gifts: $numGifts / 10"
            val spannableStringBuilder = SpannableStringBuilder(giftMsg)
            spannableStringBuilder.setSpan(boldStyle,0,giftText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            giftState.text = spannableStringBuilder
        }
        else {
            val giftText = "You don't have any gifts to open.\nTry sending your coins more often."
            val spannableStringBuilder = SpannableStringBuilder(giftText)
            spannableStringBuilder.setSpan(boldStyle,0,giftText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            giftState.text = spannableStringBuilder
        }
    }

    override fun onStart() {
        super.onStart()
        if (isNetworkAvailable()) {
            Log.d(TAG,"[onStart] User connected, load data as usual")
            loadData()
        }
        else {
            Log.d(TAG,"[onStart] User disconnected, sign out")
            signOut()
            Toast.makeText(this,"Can't communicate with server. Check your internet " +
                    "connection and log in again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadData() {
        // Get user document
        Log.d(TAG, "[loadData] Recalling wallet and gifts")
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener { task: Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val gson = Gson()
                // Load wallet
                var dataString = task.result!!.get(WALLET_KEY).toString()
                var type = object : TypeToken<ArrayList<Coin>>() {}.type
                walletList = gson.fromJson(dataString, type)
                Log.d(TAG, "[loadData] Restored wallet as $dataString")
                // Load gifts
                dataString = task.result!!.get(GIFTS_KEY).toString()
                type = object : TypeToken<ArrayList<Gift>>() {}.type
                giftList = gson.fromJson(dataString, type)
                Log.d(TAG, "[loadData] Restored gifts as $dataString")
                generateSummary()
                giftAdapter = GiftAdapter(this,giftList)
                giftListView.adapter=giftAdapter
            }
            else {
                Log.d(TAG, "[loadData] Failed to load data")
                Toast.makeText(this, "Failed to load your data, check your internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Saves data into Firestore
    private fun saveData() {
        // Update user's gifts and wallet
        val gson = Gson()
        var dataString = gson.toJson(giftList)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(GIFTS_KEY,dataString)
        Log.d(TAG, "[onStop] Stored gifts as $dataString")
        dataString = gson.toJson(walletList)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(WALLET_KEY,dataString)
        Log.d(TAG, "[onStop] Stored wallet as $dataString")
    }

    // Check if network connection is available
    private fun isNetworkAvailable() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun signOut() {
        Log.d(TAG,"[signOut] Signing out user")
        mAuth.signOut()
        val resetIntent = Intent(this, LoginActivity::class.java)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(resetIntent)
    }
}
