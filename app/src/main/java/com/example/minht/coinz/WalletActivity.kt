package com.example.minht.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
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
    private lateinit var depositStateTextView : TextView
    private lateinit var coinListView : ListView
    private lateinit var transferButton: Button
    private lateinit var depositButton: Button
    private lateinit var selectButton: Button
    private lateinit var deselectButton: Button

    private var coinList : ArrayList<Coin> = ArrayList() // User's local wallet
    // Which coins were selected for further action
    private var selectedCoinList : ArrayList<Coin> = ArrayList()

    // Banking variables
    // How many coins deposited today and exchange rates for all currencies
    private lateinit var bankAccount : BankAccount
    private var numCoinsDeposited = 0
    private var penyRate : Double = 0.0
    private var dolrRate : Double = 0.0
    private var quidRate : Double = 0.0
    private var shilRate : Double = 0.0

    // Other user variables
    private var userScore = 0.0
    private var lastTimeStamp=""

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
        const val NUM_DEPOSIT_KEY = "Number of deposited coins"
        const val SCORE_KEY = "Score"
        // Other constants
        const val MAX_COINS_LIMIT = 200 // Maximum number of coins that can be in the wallet at any time
        const val MAX_DEPOSIT = 25 // Maximum number of coins deposited per day
        const val MAX_GIFTS = 10 // Maximum number of unopened gifts one can have
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
        depositStateTextView = findViewById(R.id.deposit_state)
        coinListView = findViewById(R.id.coins_checkable_list)
        transferButton = findViewById(R.id.transfer_coins_button)
        // Send selected coins to someone
        transferButton.setOnClickListener { _ ->
            if (isNetworkAvailable()) {
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
                        if (isNetworkAvailable()) {
                            Log.d(TAG,"[onCreate] User connected, can proceed with transfer")
                            makeTransfer(usernameInput.text.toString(), userDialog)
                        }
                        else {
                            Log.d(TAG,"[onCreate] User not connected, can't proceed with transfer")
                            Toast.makeText(this,"Check your internet connection!", Toast.LENGTH_SHORT).show()
                        }
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
            else {
                Log.d(TAG,"[onCreate] User not connected, can't proceed with transfer")
                Toast.makeText(this,"Check your internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
        // Deposit selected coins
        depositButton = findViewById(R.id.deposit_coins_button)
        depositButton.setOnClickListener{ _ ->
            // If new day started, reset number of deposited coins
            // Note: new map downloaded on return to MainActivity
            val currDate = getCurrentDate()
            if (lastTimeStamp != currDate) {
                Log.d(TAG,"[onCreate] New day, reset deposit counter")
                Toast.makeText(this,"New day has started, your deposit limit has been renewed", Toast.LENGTH_SHORT).show()
                numCoinsDeposited = 0
                depositStateTextView.text = "Coins deposited today: $numCoinsDeposited / $MAX_DEPOSIT"
            }
            else {
                lastTimeStamp = currDate
            }
            getSelectedCoins()
            // Check if any coins were selected
            if (selectedCoinList.size != 0) {
                // Check daily bank limit
                if (numCoinsDeposited >= MAX_DEPOSIT) {
                    Toast.makeText(this,"Already deposited $MAX_DEPOSIT coins today, try again tomorrow!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"[onCreate] Reached daily deposit limit")
                }
                else if (numCoinsDeposited + selectedCoinList.size > MAX_DEPOSIT) {
                    val remainder = MAX_DEPOSIT - numCoinsDeposited
                    if (remainder != 1) {
                        Toast.makeText(this,"Can only deposit $remainder more coins today!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG,"[onCreate] Only 1 coin to deposit")
                    }
                    else {
                        Toast.makeText(this,"Can only deposit $remainder more coins today!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG,"[onCreate] Only $remainder coins to deposit")
                    }
                }
                else {
                    if (isNetworkAvailable()) {
                        Log.d(TAG,"[onCreate] User connected to internet, can proceed with deposit")
                        makeDeposit()
                        coinList.removeAll(selectedCoinList)
                        // Save new data for user
                        saveData()
                        selectedCoinList.clear()
                        // Update screen
                        updateScreen()
                    }
                    else {
                        Log.d(TAG,"[onCreate] User not connected, can't proceed with deposit")
                        Toast.makeText(this, "Check your internet connection!",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else {
                Toast.makeText(this, "Select some coins to deposit!", Toast.LENGTH_SHORT).show()
                Log.d(TAG,"[onCreate] No coins selected for deposit")
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
        selectedCoinList.clear() // Reset from previous
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
                                    selectedCoinList.clear()
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
        var numCoins = 0 // Number of coins deposited
        var amount = 0.0 // Deposit amount
        val contents : ArrayList<Coin> = ArrayList()
        for (coin in selectedCoinList) {
            contents.add(coin)
            numCoins++
            val currency = coin.currency
            val value = coin.valueInGold
            // Compute deposit amount using coin values and exchange rates
            when(currency) {
                "PENY" -> {
                    amount += penyRate*value
                }
                "DOLR" -> {
                    amount += dolrRate*value
                }
                "QUID" -> {
                    amount += quidRate*value
                }
                "SHIL" -> {
                    amount += shilRate*value
                }
                else -> Log.d(TAG, "[makeSelectedCoinList] Invalid currency encountered")
            }
        }
        val depositDesc : String
        depositDesc = if (numCoins != 1) {
            "Deposited $numCoins coins"
        }
        else {
            "Deposited $numCoins coin"
        }
        numCoinsDeposited += numCoins
        val newBalance = bankAccount.balance + amount
        userScore += amount
        Log.d(TAG,"[makeDeposit] New score is $userScore")
        val bankTransfer =  BankTransfer(getCurrentDate(),depositDesc,amount,newBalance,contents,true)
        bankAccount.balance = newBalance
        bankAccount.bankTransfers.add(bankTransfer)
        Log.d(TAG, "[makeDeposit] Deposit made\n" + bankTransfer.showDetails())
    }

    // Update screen after action performed (deposit or transfer)
    private fun updateScreen() {
        generateSummary()
        walletAdapter = WalletAdapter(this, coinList)
        coinListView.adapter = walletAdapter
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
                // Load bank account
                dataString = task.result!!.get(BANK_ACCOUNT_KEY).toString()
                Log.d(TAG, "[loadData] Loaded bank account as $dataString")
                bankAccount = gson.fromJson(dataString,BankAccount::class.java)
                // Load number of deposited coins today
                numCoinsDeposited = task.result!!.getLong(NUM_DEPOSIT_KEY)!!.toInt()
                Log.d(TAG,"[loadData] Loaded number of deposited coins as $numCoinsDeposited")
                // Load score
                userScore = task.result!!.getDouble(SCORE_KEY)!!
                Log.d(TAG,"[loadData] Loaded score as $userScore")
                generateSummary()
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
        val userRef =  db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userRef.update(WALLET_KEY,dataString)
        Log.d(TAG, "[onStop] Stored wallet as $dataString")
        dataString = gson.toJson(bankAccount)
        userRef.update(BANK_ACCOUNT_KEY,dataString)
        Log.d(TAG, "[onStop] Stored bank account as $dataString")
        userRef.update(NUM_DEPOSIT_KEY,numCoinsDeposited)
        Log.d(TAG, "[saveData] Stored number of deposited coins as $numCoinsDeposited")
        userRef.update(SCORE_KEY,userScore)
        Log.d(TAG, "[saveData] Stored score as $userScore")
    }

    // Summary of how many coins in wallet and number of coins deposited today before list of coins
    private fun generateSummary() {
        val numCoins = coinList.size
        if (numCoins != 0) {
            val walletStateText = "Coins in the wallet: $numCoins / $MAX_COINS_LIMIT"
            walletStateTextView.text = walletStateText
            val depositStateText = "Coins deposited today: $numCoinsDeposited / $MAX_DEPOSIT"
            depositStateTextView.text = depositStateText
        }
        else {
            val walletStateText = "Coins in the wallet: 0 / $MAX_COINS_LIMIT\nCollect coins on the" +
                    " map" + " or check for any unopened gifts!"
            walletStateTextView.text = walletStateText
            val depositStateText = "Coins deposited today: $numCoinsDeposited / $MAX_DEPOSIT"
            depositStateTextView.text = depositStateText
        }
    }

    // Check if internet connection is available
    private fun isNetworkAvailable() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    override fun onStart() {
        super.onStart()
        if (isNetworkAvailable()) {
            Log.d(TAG,"[onStart] User connected, start as usual")
            lastTimeStamp = getCurrentDate()
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
        else {
            Log.d(TAG,"[onStart] User disconnected, sign out")
            signOut()
            Toast.makeText(this,"Can't communicate with server. Check your internet " +
                    "connection and log in again.", Toast.LENGTH_SHORT).show()
        }
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