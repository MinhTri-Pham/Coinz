package com.example.minht.coinz

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
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
    // Screen components
    private lateinit var walletAdapter : WalletAdapter
    private lateinit var walletStateTextView : TextView
    private lateinit var coinListView : ListView
    private lateinit var transferButton: Button
    private lateinit var depositButton: Button
    private lateinit var selectButton: Button
    private lateinit var deselectButton: Button

    private var coinList : ArrayList<Coin> = ArrayList() // User's local wallet
    // Which coins were selected for further action
    private var selectedCoinList : ArrayList<Coin> = ArrayList()

    // Banking variables
    // How many coins selected per day and exchange rates for all currencies
    private lateinit var bankAccount : BankAccount
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
        const val SELECTED_COINS_KEY="Selected coins" // For next screen when sending coins to other players
        // For accessing data in Firestore
        const val COLLECTION_KEY = "Users"
        const val WALLET_KEY = "Wallet"
        const val BANK_ACCOUNT_KEY = "Bank"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        // Handle button click
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        gson = Gson()
        walletStateTextView = findViewById(R.id.wallet_state)
        coinListView = findViewById(R.id.coins_checkable_list)
        transferButton = findViewById(R.id.transfer_coins_button)
        // Send selected coins to someone
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
                selectedCoinList.clear()
            } else {
                Toast.makeText(this, "Select some coins to transfer!", Toast.LENGTH_SHORT).show()
                Log.d(TAG,"[onCreate] No coins selected for transfer")
            }
        }
        // Deposit selected coins
        depositButton = findViewById(R.id.deposit_coins_button)
        depositButton.setOnClickListener{ _ ->
            // Check daily banking limit
            if (numberCoinsBanked < 25) {
                getSelectedCoins()
                // Check if any coins were selected
                if (selectedCoinList.size != 0) {
                    makeDeposit()
                    coinList.removeAll(selectedCoinList)
                    // Save new data for user
                    saveData()
                    // Update top summary, remove deposited coins from screen
                    generateSummary()
                    walletAdapter = WalletAdapter(this, coinList)
                    coinListView.adapter = walletAdapter
                    selectedCoinList.clear()
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
        // Check all coins
        selectButton = findViewById(R.id.select_all_button)
        selectButton.setOnClickListener {_ : View ->
            for (coin in coinList) {
                coin.selected = true
                selectedCoinList.clear()
                selectedCoinList.addAll(coinList)
                walletAdapter = WalletAdapter(this, coinList)
                coinListView.adapter = walletAdapter
            }
        }
        // Uncheck all coins
        deselectButton = findViewById(R.id.deselect_all_button)
        deselectButton.setOnClickListener { _:View ->
            for (coin in coinList) {
                coin.selected = false
                selectedCoinList.clear()
                walletAdapter = WalletAdapter(this, coinList)
                coinListView.adapter = walletAdapter
            }
        }
    }

    // Extract which coins were selected
    private fun getSelectedCoins() {
        for (coin in coinList) {
            if (coin.selected) {
                selectedCoinList.add(coin)
                coin.selected = false // Reset selected value for possible recipient
            }
        }
    }

    // Make bank deposit from selected coins
    private fun makeDeposit() {
        var amount = 0.0
        // Counts per currency selected
        var numPenySelected = 0
        var numDolrSelected = 0
        var numQuidSelected = 0
        var numShilSelected = 0
        for (coin in selectedCoinList) {
            val currency = coin.currency
            val value = coin.valueInGold
            // Compute how many coins for each currency were computed
            // Compute deposit amount using coin values and exchange rates
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
        val depositDesc = "Deposited $numPenySelected PENY, $numDolrSelected DOLR, $numQuidSelected QUID and $numShilSelected SHIL"
        val newBalance = bankAccount.balance + amount
        val bankTransfer =  BankTransfer(getCurrentDate(),depositDesc,amount,newBalance)
        bankAccount.balance = newBalance
        bankAccount.bankTransfers.add(bankTransfer)
        Log.d(TAG, "[makeDeposit] Deposit made\n" + bankTransfer.toString())
    }

    // Returns today's date in format: YYYY/MM/DD
    private fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val result = sdf.format(Date())
        Log.d(TAG, "[getCurrentDate]: current date is $result")
        return result
    }

    // Loads data in Firestore
    private fun loadData() {
        // Get user document
        Log.d(TAG, "[loadData] Recalling wallet and bank account")
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener { task: Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                // Load wallet
                var dataString = task.result!!.get(WALLET_KEY).toString()
                Log.d(TAG, "[loadData] Loaded wallet as $dataString")
                if (dataString == "[]") {
                    Log.d(TAG, "[loadData] No coins collected yet")
                    coinList = ArrayList()

                } else {
                    val type = object : TypeToken<ArrayList<Coin>>() {}.type
                    coinList = gson.fromJson(dataString, type)
                    Log.d(TAG, "[loadData] Restored coins")
                    Log.d(TAG, "[loadData] Displaying list of coins in the wallet")
                    walletAdapter = WalletAdapter(this, coinList)
                    coinListView.adapter = walletAdapter
                }
                generateSummary()
                // Load bank account
                dataString = task.result!!.get(BANK_ACCOUNT_KEY).toString()
                Log.d(TAG, "[loadData] Loaded bank account as $dataString")
                bankAccount = gson.fromJson(dataString,BankAccount::class.java)
            }
            else {
                Log.d(TAG, "[loadData] Failed to load data")
                Toast.makeText(this, "Failed to load your data, check your internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Saves data into Firestore
    private fun saveData() {
        // Update user's wallet and bank account
        var dataString = gson.toJson(coinList)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(WALLET_KEY,dataString)
        Log.d(TAG, "[onStop] Stored wallet as $dataString")
        dataString = gson.toJson(bankAccount)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(BANK_ACCOUNT_KEY,dataString)
        Log.d(TAG, "[onStop] Stored bank account as $dataString")
    }

    // Generate summary before list of coins
    private fun generateSummary() {
        val numCoins = coinList.size
        if (numCoins != 0) {
            val walletStateText = "Coins in the wallet: $numCoins / 1000"
            walletStateTextView.text = walletStateText
        }
        else {
            val walletStateText = "Coins in the wallet: 0 / 1000\nCollect coins on the" +
                    " map" + " or check for any unopened gifts!"
            walletStateTextView.text = walletStateText
        }
    }

    override fun onStart() {
        super.onStart()
        // Load exchange rates from Shared Preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        penyRate = settings.getString("penyRate","0.0").toDouble()
        dolrRate = settings.getString("dolrRate","0.0").toDouble()
        quidRate = settings.getString("quidRate","0.0").toDouble()
        shilRate = settings.getString("shilRate","0.0").toDouble()
        Log.d(TAG, "[onStart] Recalled PENY rate as $penyRate")
        Log.d(TAG, "[onStart] Recalled DOLR rate as $dolrRate")
        Log.d(TAG, "[onStart] Recalled QUID rate as $quidRate")
        Log.d(TAG, "[onStart] Recalled SHIL rate as $shilRate")
        loadData() // Other data loaded from Firestore
    }
}