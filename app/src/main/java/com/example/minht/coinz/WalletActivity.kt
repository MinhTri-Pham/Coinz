package com.example.minht.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WalletActivity : AppCompatActivity() {

    private var coinList : ArrayList<Coin> = ArrayList()
    private var displayCoinList : ArrayList<String> = ArrayList()
    private var selectedDisplayCoinsList : ArrayList<String> = ArrayList()
    private lateinit var viewAdapter : ArrayAdapter<String>
    private lateinit var coinListView : ListView
    private lateinit var sendButton: Button

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var gson : Gson

    companion object {
        const val TAG = "WalletActivity" // For logging purposes
        const val COLLECTION_KEY = "Users"
        const val WALLET_KEY = "Wallet"
        const val SELECTED_COINS_KEY="Selected coins"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        // Handle button click
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        gson = Gson()
        sendButton = findViewById(R.id.send_coins_button)
        sendButton.setOnClickListener { _ ->
            // Check if any coins were selected before proceeding to the next screen
            if (selectedDisplayCoinsList.size != 0) {
                // Pass JSON representation of selected coins to next activity
                val selectedCoinListJSON = makeSelectedCoinList(selectedDisplayCoinsList)
                val selectRecipientIntent = Intent(this,SelectRecipientActivity::class.java)
                selectRecipientIntent.putExtra(SELECTED_COINS_KEY,selectedCoinListJSON)
                startActivity(selectRecipientIntent)
            } else {
                Toast.makeText(this, "Select some coins for transfer!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // Prepare list for displaying coins
    private fun makeDisplayCoinList() {
        for (coin in coinList) {
            displayCoinList.add(coin.toString())
        }
    }

    // Displays state of the wallet as a checkbox list of coins
    private fun displayCoins() {
        val walletStateTextView = findViewById<TextView>(R.id.wallet_state)
        val numCoins = coinList.size
        if (numCoins == 0) {
            Log.d(TAG, "[displayCoins] No coins to display")
            val walletStateText = "Coins in the wallet: 0 / 1000 \n Collect coins on the map" +
            " or check for any unopened gifts!"
            walletStateTextView.text = walletStateText
        } else {
            val walletStateText = "Coins in the wallet: $numCoins / 1000"
            walletStateTextView.text = walletStateText
            coinListView = findViewById(R.id.coins_checkable_list)
            coinListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            viewAdapter = ArrayAdapter(this,R.layout.row_layout,R.id.text_checkbox,displayCoinList)
            coinListView.adapter = viewAdapter
            // Listen for which items are selected
            coinListView.setOnItemClickListener { _, view, _, _ ->
                val selectedItemTextView = view as TextView
                val selectedItem = selectedItemTextView.text.toString()
                if (selectedDisplayCoinsList.contains(selectedItem)) {
                    selectedDisplayCoinsList.remove(selectedItem) // Unchecking item
                } else {
                    selectedDisplayCoinsList.add(selectedItem)
                }
            }
            Log.d(TAG, "[displayCoins] Coins to displayed")
        }
    }

    // Make JSON representation of selected coins from their string representation
    private fun makeSelectedCoinList(coinStrings : ArrayList<String>) : String {
        val selectedCoinList : ArrayList<Coin> = ArrayList()
        for (coinString in coinStrings) {
            val currency = coinString.substring(0,4)
            val value = coinString.substring(coinString.indexOf(":")+2,coinString.indexOf(":")+7).toDouble()
            val id = coinString.substring(coinString.indexOf("Id:")+4)
            val selectedCoin = Coin(id,currency,value)
            selectedCoinList.add(selectedCoin)
        }
        return gson.toJson(selectedCoinList)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG,"[onStart] Recalling list of coins in the wallet")
        // Find JSON representation of user's wallet in FireStore
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener{ task : Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val walletString = task.result!!.get(WALLET_KEY).toString()
                Log.d(TAG,"[onStart] JSON representation of wallet $walletString")
                if (walletString.equals("[]")) {
                    Log.d(TAG,"[onStart] No coins collected yet")
                    coinList = ArrayList()

                } else {
                    val type = object : TypeToken<ArrayList<Coin>>(){}.type
                    coinList = gson.fromJson(walletString, type)
                    Log.d(TAG,"[onStart] Restored coins")
                    makeDisplayCoinList()
                    Log.d(TAG,"[onStart] Displaying list of coins in the wallet")
                }
                displayCoins()
            } else {
                Log.d(TAG,"[onStart] Failed to extract JSON representation of wallet state")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val json = gson.toJson(coinList)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(WALLET_KEY,json)
        Log.d(TAG, "[onStop] Stored wallet state as $json")
    }
}
