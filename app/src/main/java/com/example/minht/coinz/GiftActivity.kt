package com.example.minht.coinz

import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
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
            val selectedGift = giftList[pos]
            // If no space in wallet, warn user
            if (walletList.size > MAX_COINS_LIMIT - selectedGift.contents.size) {
                Toast.makeText(this, "Don't have enough space in your wallet!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "[onCreate] Can't open gift, since there's not enough space in the wallet")
            }
            else {
                val giftPrompt = AlertDialog.Builder(this)
                val giftMsg = "Confirm opening gift with contents:\n\n" + selectedGift.showContents()
                giftPrompt.setTitle("Gift confirmation").setMessage(giftMsg)
                // If "Open gift" is pressed, process accordingly
                // See openGifts function for details
                giftPrompt.setPositiveButton("Open gift") { _: DialogInterface?, _: Int ->
                    openGift(selectedGift)
                }
                // If "Cancel" is pressed, dialog closes and no changes take place
                giftPrompt.setNegativeButton("Cancel") { _, _ ->}
                giftPrompt.show()
            }
        }
    }

    // Add contents of gifts to wallet and update screen
    private fun openGift(gift: Gift) {
        val coins = gift.contents
        giftList.remove(gift)
        walletList.addAll(coins)
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
        if (numGifts != 0) {
            giftState.text = "Unopened gifts: $numGifts / 10"
        }
        else {
            giftState.text = "No gifts to open"
        }
    }

    override fun onStart() {
        super.onStart()
        loadData()
//        // BEGIN TEST
//        val testCoin1 = Coin("aa","PENY", 2.718281, false)
//        val testCoin2 = Coin("bb", "QUID", 3.141592, false)
//        val testGift1Contents = ArrayList<Coin>()
//        testGift1Contents.add(testCoin1)
//        testGift1Contents.add(testCoin2)
//        val testGift1 = Gift("2018/11/16", "DummyGuy", testGift1Contents)
//
//        val testCoin3 = Coin("cc","SHIL", 4.214315, false)
//        val testCoin4 = Coin("dd", "SHIL", 9.821423, false)
//        val testCoin5 = Coin("ee", "DOLR", 0.122142, false)
//        val testGift2Contents = ArrayList<Coin>()
//        testGift2Contents.add(testCoin3)
//        testGift2Contents.add(testCoin4)
//        testGift2Contents.add(testCoin5)
//        val testGift2 = Gift("2018/11/19", "AnotherDummyGuy", testGift2Contents)
//
//        giftList.add(testGift1)
//        giftList.add(testGift2)
        // END TEST
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
                Log.d(TAG, "[loadData] Restored wallet")
                // Load gifts
                dataString = task.result!!.get(GIFTS_KEY).toString()
                type = object : TypeToken<ArrayList<Gift>>() {}.type
                giftList = gson.fromJson(dataString, type)
                Log.d(TAG, "[loadData] Restored gifts")
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
}
