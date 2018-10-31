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

    private val tag = "WalletActivity" // For logging purposes
    //private val preferencesFile = "MyPrefsFile"
    private var coinList : ArrayList<Coin> = ArrayList()
    private var displayCoinList : ArrayList<String> = ArrayList()
    private var selectedCoinsList : ArrayList<String> = ArrayList()
    private lateinit var viewAdapter : ArrayAdapter<String>
    private lateinit var coinListView : ListView
    private lateinit var sendButton: Button

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private var COLLECTION_KEY = "Users"
    private var WALLET_KEY = "Wallet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        // Handle button click
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sendButton = findViewById(R.id.send_coins_button)
        sendButton.setOnClickListener { _ ->
            // Check if any coins were selected before proceeding to the next screen
            if (selectedCoinsList.size != 0) {
                startActivity(Intent(this, SelectRecipientActivity::class.java))
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
        val numCoins = displayCoinList.size
        if (numCoins == 0) {
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
            coinListView.setOnItemClickListener { adapterView, view, i, l ->
                val selectedItemTextView = view as TextView
                val selectedItem = selectedItemTextView.text.toString()
                if (selectedCoinsList.contains(selectedItem)) {
                    selectedCoinsList.remove(selectedItem) // Unchecking item
                } else {
                    selectedCoinsList.add(selectedItem)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(tag,"[onStart] Recalling list of coins in the wallet")
        val gson = Gson()
        // Find JSON representation of user's wallet in FireStore
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener{ task : Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val walletString = task.result!!.get(WALLET_KEY).toString()
                Log.d(tag,"[onStart] JSON representation of wallet $walletString")
                if (walletString.equals("[]")) {
                    Log.d(tag,"[onStart] No coins collected yet")
                    coinList = ArrayList()
                } else {
                    val type = object : TypeToken<ArrayList<Coin>>(){}.type
                    Log.d(tag,"[onStart] Restored coins")
                    coinList = gson.fromJson(walletString, type)
                    makeDisplayCoinList()
                    Log.d(tag,"[onStart] Displaying list of coins in the wallet")
                    displayCoins()
                }

            } else {
                Log.d(tag,"[onStart] Failed to extract JSON representation of wallet state")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(tag,"[onStop] Storing list of coins in the wallet")
        val gson = Gson()
        var json = ""
        if (!coinList.isEmpty()) {
            json = gson.toJson(coinList)
        }
        val walletState : HashMap<String, Any> = HashMap();
        walletState.put(WALLET_KEY,json)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(walletState)

    }
}
