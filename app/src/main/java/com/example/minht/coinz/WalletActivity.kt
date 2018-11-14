package com.example.minht.coinz

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*

class WalletActivity : AppCompatActivity() {
    // Variables regarding coins in the wallet and screen components
    private var coinList : ArrayList<Coin> = ArrayList()
    private var selectedCoinList : ArrayList<Coin> = ArrayList()
    private lateinit var walletAdapter : WalletAdapter
    private lateinit var walletStateTextView : TextView
    private lateinit var coinListView : ListView
    private lateinit var transferButton: Button
    private lateinit var depositButton: Button

    // Counts per currency selected
    private var numPenySelected = 0
    private var numDolrSelected = 0
    private var numQuidSelected = 0
    private var numShilSelected = 0

    // Banking variables
    // How many coins selected per day and exchange rates for all currencies
    //private var bankTransferList : ArrayList<BankTransfer> = ArrayList()
    private var bankBalance = 0.0
    private var numberCoinsBanked = 0
    private var penyRate : Double = 0.0
    private var dolrRate : Double = 0.0
    private var quidRate : Double = 0.0
    private var shilRate : Double = 0.0

    // Other general variables
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var gson : Gson
    private val preferencesFile = "MyPrefsFile" // For storing preferences

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
        coinListView = findViewById(R.id.coins_checkable_list)
        transferButton = findViewById(R.id.transfer_coins_button)
        transferButton.setOnClickListener { _ ->
            getSelectedCoins()
            // Check if any coins were selected before proceeding to the next screen
            if (selectedCoinList.size != 0) {
                val selectedCoinListJSON = gson.toJson(selectedCoinList)
                val selectRecipientIntent = Intent(this,SelectRecipientActivity::class.java)
                selectRecipientIntent.putExtra(SELECTED_COINS_KEY,selectedCoinListJSON)
                Log.d(TAG, "[onCreate] Some coins selected and existing user identified, proceeding to next screen")
                startActivity(selectRecipientIntent)
                walletAdapter = WalletAdapter(this, coinList)
                coinListView.adapter = walletAdapter
                cleanUp()
            } else {
                Toast.makeText(this, "Select some coins to transfer!", Toast.LENGTH_SHORT).show()
                Log.d(TAG,"[onCreate] No coins selected for transfer")
            }
        }
        depositButton = findViewById(R.id.deposit_coins_button)
        depositButton.setOnClickListener{ _ ->
            // Check daily banking limit
            if (numberCoinsBanked < 25) {
                getSelectedCoins()
                // Check if any coins were selected
                if (selectedCoinList.size != 0) {
                    val bankTransfer = makeBankTransfer()
                    Log.d(TAG, "[onCreate] Deposit made\n" + bankTransfer.toString())
                    // Reset screen
                    coinList.removeAll(selectedCoinList)
                    walletAdapter = WalletAdapter(this, coinList)
                    coinListView.adapter = walletAdapter
                    cleanUp()
                }
                else {
                    Toast.makeText(this, "Select some coins to deposit!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"[onCreate] No coins selected for deposit")
                }
            }
            else {
                Toast.makeText(this,"Already banked 25 coins today, try again tomorrow!", Toast.LENGTH_SHORT).show()
                Log.d(TAG,"[onCreate] Reached banking limit")
            }

        }
        walletStateTextView = findViewById(R.id.wallet_state)
    }

    // Extract which coins were selected
    // Also make counts per currency
    private fun getSelectedCoins() {
        for (coin in coinList) {
            if (coin.selected) {
                selectedCoinList.add(coin)
                coin.selected = false // Reset selected value for possible recipient
            }
        }
    }

    // Make bank deposit from selected coins
    private fun makeBankTransfer() : BankTransfer {
        var amount = 0.0
        for (coin in selectedCoinList) {
            val currency = coin.currency
            val value = coin.valueInGold
            when(currency) {
                "PENY" -> {
                    numPenySelected++
                    amount += penyRate*value
                }
                "DOLR" -> {
                    numDolrSelected++
                    amount += dolrRate*value
                }
                "QUID" -> {
                    numQuidSelected++
                    amount += quidRate*value
                }
                "SHIL" -> {
                    numShilSelected++
                    amount += shilRate*value
                }
                else -> Log.d(TAG, "[makeSelectedCoinList] Invalid currency encountered")
            }
        }
        val depositDesc = "Deposite $numPenySelected PENY, $numDolrSelected DOLR, $numQuidSelected QUID and $numShilSelected SHIL"
        bankBalance += amount
        return BankTransfer(getCurrentDate(),depositDesc,amount,bankBalance)
    }

    // Clean up resources
    private fun cleanUp() {
        selectedCoinList.clear()
        numPenySelected = 0
        numDolrSelected = 0
        numQuidSelected = 0
        numShilSelected = 0
    }

    // Returns today's date in format: YYYY/MM/DD
    private fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val result = sdf.format(Date())
        Log.d(TAG, "[getCurrentDate]: current date is $result")
        return result
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "[onStart] Recalling list of coins in the wallet")
        // Find JSON representation of user's wallet in FireStore
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener { task: Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val walletString = task.result!!.get(WALLET_KEY).toString()
                Log.d(TAG, "[onStart] JSON representation of wallet $walletString")
                if (walletString.equals("[]")) {
                    Log.d(TAG, "[onStart] No coins collected yet")
                    coinList = ArrayList()
                    val walletStateText = "Coins in the wallet: 0 / 1000 \n Collect coins on the" +
                            " map" + " or check for any unopened gifts!"
                    walletStateTextView.text = walletStateText

                } else {
                    val type = object : TypeToken<ArrayList<Coin>>() {}.type
                    coinList = gson.fromJson(walletString, type)
                    val walletStateText = "Coins in the wallet: ${coinList.size} / 1000"
                    walletStateTextView.text = walletStateText
                    Log.d(TAG, "[onStart] Restored coins")
                    Log.d(TAG, "[onStart] Displaying list of coins in the wallet")
                    walletAdapter = WalletAdapter(this, coinList)
                    coinListView.adapter = walletAdapter
                }
            }
        }
        // Load exchange rates
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        penyRate = settings.getString("penyRate","0.0").toDouble()
        dolrRate = settings.getString("dolrRate","0.0").toDouble()
        quidRate = settings.getString("quidRate","0.0").toDouble()
        shilRate = settings.getString("shilRate","0.0").toDouble()
        Log.d(TAG, "[onStop] Recalled PENY rate as $penyRate")
        Log.d(TAG, "[onStop] Recalled DOLR rate as $dolrRate")
        Log.d(TAG, "[onStop] Recalled QUID rate as $quidRate")
        Log.d(TAG, "[onStop] Recalled SHIL rate as $shilRate")
    }

    override fun onStop() {
        super.onStop()
        val json = gson.toJson(coinList)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(WALLET_KEY,json)
        Log.d(TAG, "[onStop] Stored wallet state as $json")
    }
}