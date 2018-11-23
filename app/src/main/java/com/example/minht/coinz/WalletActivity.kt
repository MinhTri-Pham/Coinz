package com.example.minht.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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

    //private var closeDialog = false
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

    companion object {
        const val PREFS_FILE = "MyPrefsFile" // Storing data
        const val TAG = "WalletActivity" // Logging purposes
        // For accessing data in Firestore
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY = "Username"
        const val WALLET_KEY = "Wallet"
        const val GIFTS_KEY = "Gifts"
        const val BANK_ACCOUNT_KEY = "Bank"
        const val MAX_GIFTS = 2 // Maximum number of unopened gifts one can have
    }

    @SuppressLint("InflateParams")
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
            // If some coins collected, open a prompt input dialog to type username of recipient directly
            if (selectedCoinList.size != 0) {
                val layoutInflater = LayoutInflater.from(this)
                val promptView = layoutInflater.inflate(R.layout.username_prompt,null)
                val usernamePrompt = AlertDialog.Builder(this)
                val usernameInput = promptView.findViewById(R.id.recipient_username_editText) as EditText
                usernamePrompt.setView(promptView)
                usernamePrompt.setTitle("Choose transfer recipient").setCancelable(false)
                usernamePrompt.setPositiveButton("Confirm transfer") { _:DialogInterface, _:Int ->}
                usernamePrompt.setNegativeButton("Cancel") { _:DialogInterface, _:Int ->}
                val userDialog = usernamePrompt.create()
                userDialog.show()
                // Pressing "Confirm transfer" processes this transfer
                // Details in the makeTransfer function
                userDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _: View ->
                    makeTransfer(usernameInput.text.toString(), userDialog)
                }
                // Pressing "Cancel" closes the prompt dialog and all coins are deselected
                userDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { _:View ->
                    Log.d(TAG,"[onCreate] Transfer cancelled")
                    deselectAll()
                    userDialog.dismiss()
                }
            }
            else {
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
                    // Update screen
                    updateScreen()
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
            selectAll()
        }
        deselectButton = findViewById(R.id.deselect_all_button)
        deselectButton.setOnClickListener { _:View ->
            deselectAll()
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

    // Select all coins
    private fun selectAll() {
        for (coin in coinList) {
            coin.selected = true
            selectedCoinList.clear()
            selectedCoinList.addAll(coinList)
            walletAdapter = WalletAdapter(this, coinList)
            coinListView.adapter = walletAdapter
        }
    }

    // Deselect all coins
    private fun deselectAll() {
        for (coin in coinList) {
            coin.selected = false
            selectedCoinList.clear()
            walletAdapter = WalletAdapter(this, coinList)
            coinListView.adapter = walletAdapter
        }
    }

    // Transfer selected coins to given user
    private fun makeTransfer(recipientUsername: String, dialog: AlertDialog) {
        val usersRef = db.collection(COLLECTION_KEY)
        val userDoc = usersRef.document(mAuth.uid!!)
        userDoc.get().addOnCompleteListener{task: Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val mUsername = task.result!!.getString(USERNAME_KEY) // Username of current user
                if (!recipientUsername.isEmpty()) {
                    val findUsernameQuery = usersRef.whereEqualTo(USERNAME_KEY,recipientUsername)
                    findUsernameQuery.get().addOnCompleteListener{taskQuery: Task<QuerySnapshot> ->
                        if (taskQuery.isSuccessful) {
                            val doc = taskQuery.result
                            // Check if chosen username exists
                            if (!doc!!.isEmpty) {
                                // If input valid, process appropriately and close the prompt dialog
                                val recipientId = doc.documents[0].id
                                var recipientGiftsString = doc.documents[0].get(GIFTS_KEY).toString()
                                val type = object : TypeToken<ArrayList<Gift>>() {}.type
                                val recipientGifts : ArrayList<Gift> = gson.fromJson(recipientGiftsString,type)
                                if (recipientGifts.size < MAX_GIFTS) {
                                    val gift = Gift(getCurrentDate(), mUsername!!, selectedCoinList)
                                    recipientGifts.add(gift)
                                    recipientGiftsString = gson.toJson(recipientGifts)
                                    usersRef.document(recipientId).update(GIFTS_KEY,recipientGiftsString)
                                    Log.d(TAG, "[makeTransfer] Updated wallet of recipient as $recipientGiftsString")
                                    coinList.removeAll(selectedCoinList)
                                    val userWalletString = gson.toJson(coinList)
                                    usersRef.document(mAuth.uid!!).update(WALLET_KEY,userWalletString)
                                    Toast.makeText(this, "Transfer completed", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss() // Close prompt dialog
                                    updateScreen() // Refresh screen
                                }
                                else {
                                    // If chosen person already has max number of unopened gifts, warn user
                                    // Don't close prompt window
                                    Log.d(TAG, "[makeTransfer] Input user already has maximum number of gifts")
                                    Toast.makeText(this, "Chosen user already has maximum number of gifts! Try a different one", Toast.LENGTH_SHORT).show()
                                }

                            }
                            else {
                                // If invalid input, warn user and don't close the prompt window
                                Log.d(TAG, "[makeTransfer] Input user doesn't exist")
                                Toast.makeText(this, "Entered username doesn't exist! Try a different one", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else {
                            Log.d(TAG,"[makeTransfer] Error getting data")
                        }
                    }
                }
                else {
                    // If blank input, warn user and don't close the prompt window
                    Log.d(TAG,"[makeTransfer] Can't query since input was empty")
                    Toast.makeText(this, "Choose a player to send coins to", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(this,"Transfer failed, error with loading data", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "[makeTransfer] Error loading data")
            }
        }
    }

    // Deposit selected coins to bank account
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

    // Update screen after action performed (deposit or transfer)
    private fun updateScreen() {
        generateSummary()
        walletAdapter = WalletAdapter(this, coinList)
        coinListView.adapter = walletAdapter
        selectedCoinList.clear()
    }

    // Returns today's date in format YYYY/MM/DD
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
        val settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
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