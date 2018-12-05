package com.example.minht.coinz

import android.annotation.SuppressLint
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
    private lateinit var coinListDesc : TextView
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
    private lateinit var ratesDesc: TextView
    private lateinit var penyRateTextView: TextView
    private lateinit var dolrRateTextView: TextView
    private lateinit var quidRateTextView: TextView
    private lateinit var shilRateTextView: TextView
    private var lastTimeStamp="" // Last time deposit was attempted (for resetting purposes)
    private var ratesToDateFlag = false

    // Other user variables
    private var userName = ""
    private var userScore = 0.0

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
        // Initialise summary views
        walletStateTextView = findViewById(R.id.wallet_state)
        depositStateTextView = findViewById(R.id.deposit_state)
        coinListDesc = findViewById(R.id.coinListDesc)
        coinListView = findViewById(R.id.coin_checkable_list)
        transferButton = findViewById(R.id.transfer_coins_button)
        ratesDesc = findViewById(R.id.ratesDesc)
        penyRateTextView = findViewById(R.id.penyRateInfo)
        dolrRateTextView = findViewById(R.id.dolrRateInfo)
        quidRateTextView = findViewById(R.id.quidRateInfo)
        shilRateTextView = findViewById(R.id.shilRateInfo)
        // Send selected coins to someone
        transferButton.setOnClickListener { _ ->
            getSelectedCoins() // Make sure some coins were selected
            if (selectedCoinList.size != 0) {
                // Open an input dialog to type username of recipient directly
                val layoutInflater = LayoutInflater.from(this)
                val promptView = layoutInflater.inflate(R.layout.username_prompt,null)
                val usernamePrompt = AlertDialog.Builder(this,R.style.MyDialogTheme)
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
                        Toast.makeText(this,"Can't make transfer, " +
                                "check your internet connection!", Toast.LENGTH_SHORT).show()
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
                // Warn user if no coins selected
                Toast.makeText(this, "Select some coins to transfer!", Toast.LENGTH_SHORT).show()
                Log.d(TAG,"[onCreate] No coins selected for transfer")
            }
        }
        depositButton = findViewById(R.id.deposit_coins_button)
        depositButton.setOnClickListener{ _ ->
            // If new day started and user not notified about it yet,
            // reset number of deposited coins and notify user.
            // Note: new map downloaded on return to MainActivity
            val currDate = getCurrentDate()
            if (lastTimeStamp != currDate && !ratesToDateFlag) {
                Log.d(TAG,"[onCreate] Map expired, need to update map and rates")
                Toast.makeText(this,"Current rates no longer valid, deposits can't be made. " +
                        "Go back to the map to update them ", Toast.LENGTH_SHORT).show()
                ratesToDateFlag = true // Make sure user won't be notified again about same issue
                numCoinsDeposited = 0
                depositStateTextView.text = "Coins deposited today: 0 / $MAX_DEPOSIT"
            }
            else {
                lastTimeStamp = currDate // Update last timestamp when deposit was attempted
                getSelectedCoins() // Make sure some coins were selected
                if (selectedCoinList.size != 0) {
                    val numSelectedCollected = getSelectedCollectedCoins()
                    // Check daily deposit limit on collected coins
                    if (numCoinsDeposited >= MAX_DEPOSIT) {
                        // Limit already reached
                        Toast.makeText(this,"Reached daily deposit limit on collected coins today! " +
                                "Try again tomorrow or deposit any received coins!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG,"[onCreate] Reached daily deposit limit")
                    }
                    else if (numCoinsDeposited + numSelectedCollected > MAX_DEPOSIT) {
                        // Deposit would go over the limit
                        val remainder = MAX_DEPOSIT - numCoinsDeposited
                        // Show many more coins can be deposited, distinguish singular/plural
                        if (remainder != 1) {
                            Toast.makeText(this,"Can only deposit $remainder more collected coins today!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG,"[onCreate] Only $remainder collected coins left to deposit")
                        }
                        else {
                            Toast.makeText(this,"Can only deposit 1 more collected coin today!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG,"[onCreate] Only 1 coin to deposit")
                        }
                    }
                    else {
                        if (isNetworkAvailable()) {
                            // Provided network is available, process deposit
                            // Details in the makeDeposit function
                            Log.d(TAG,"[onCreate] User connected to internet, can proceed with deposit")
                            makeDeposit()
                            saveData() // Save changed wallet and bank account
                            selectedCoinList.clear() // Reset for next action
                            updateScreen()
                        }
                        else {
                            Log.d(TAG,"[onCreate] User not connected, can't proceed with deposit")
                            Toast.makeText(this, "Can't deposit, check your internet connection!",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else {
                    // Warn user if no coins selected
                    Toast.makeText(this, "Select some coins to deposit!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"[onCreate] No coins selected for deposit")
                }
            }
        }
        selectButton = findViewById(R.id.select_all_button)
        selectButton.setOnClickListener {_ : View ->
            selectAll()
        }
        deselectButton = findViewById(R.id.deselect_all_button)
        deselectButton.setOnClickListener { _:View ->
            deselectAll()
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
                    // Make sure username inputted by user exists
                    val findUsernameQuery = usersRef.whereEqualTo(USERNAME_KEY,recipientUsername)
                    findUsernameQuery.get().addOnCompleteListener{taskQuery: Task<QuerySnapshot> ->
                        if (taskQuery.isSuccessful) {
                            val doc = taskQuery.result
                            if (!doc!!.isEmpty) {
                                // Can't send coins to yourself, don't close prompt window
                                if (recipientUsername == userName) {
                                    Log.d(TAG, "[makeTransfer] Attempting to send coins to yourself")
                                    Toast.makeText(this,"Can't send coins to yourself!",Toast.LENGTH_SHORT).show()
                                }
                                else {
                                    // Extracts gifts of recipient
                                    val recipientId = doc.documents[0].id
                                    var recipientGiftsString = doc.documents[0].get(GIFTS_KEY).toString()
                                    val type = object : TypeToken<ArrayList<Gift>>() {}.type
                                    val recipientGifts : ArrayList<Gift> = gson.fromJson(recipientGiftsString,type)
                                    if (recipientGifts.size < MAX_GIFTS) {
                                        val giftContents = makeGiftContents() // Make gift from selected coins
                                        val gift = Gift(getCurrentDate(), mUsername!!, giftContents)
                                        // Add gift to recipient's gifts and update in Firestore
                                        recipientGifts.add(gift)
                                        recipientGiftsString = gson.toJson(recipientGifts)
                                        usersRef.document(recipientId).update(GIFTS_KEY,recipientGiftsString)
                                        Log.d(TAG, "[makeTransfer] Updated wallet of recipient as $recipientGiftsString")
                                        // Remove coins from user wallet and update in Firestore
                                        coinList.removeAll(giftContents)
                                        val userWalletString = gson.toJson(coinList)
                                        usersRef.document(mAuth.uid!!).update(WALLET_KEY,userWalletString)
                                        Toast.makeText(this, "Transfer completed", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss() // Close prompt dialog
                                        selectedCoinList.clear()
                                        updateScreen() // Refresh screen without selected coins
                                    }
                                    else {
                                        // If chosen person already has max number of unopened gifts, warn user
                                        // Don't close prompt window
                                        Log.d(TAG, "[makeTransfer] Input user already has maximum number of gifts")
                                        Toast.makeText(this, "Chosen user already has maximum number of gifts! Try a different one", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            else {
                                // Warn user if input username doesn't exist
                                Log.d(TAG, "[makeTransfer] Input user doesn't exist")
                                Toast.makeText(this, "No player with such username! Try a different one", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else {
                            val message = taskQuery.exception!!.message
                            Log.d(TAG,"[makeTransfer] Error when looking for username")
                            Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else {
                    // If empty input, warn user and don't close the prompt window
                    Log.d(TAG,"[makeTransfer] Can't query since input was empty")
                    Toast.makeText(this, "Choose a player to send coins to", Toast.LENGTH_SHORT).show()
                }
            }
            // If some error occurs, warn user and close dialog
            else {
                Log.d(TAG, "[makeTransfer] Error getting user document")
                val message = task.exception!!.message
                Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                deselectAll()
            }
        }
    }

    // Deposit selected coins to bank account
    private fun makeDeposit() {
        var numCoins = 0 // Total number of coins in deposit
        var numCollectedCoins = 0 // Number of coins collected by user in deposit
        var amount = 0.0 // Deposit amount
        val contents : ArrayList<Coin> = ArrayList()
        for (coin in selectedCoinList) {
            contents.add(coin)
            numCoins++
            if (coin.collected) {
                numCollectedCoins++
            }
            val currency = coin.currency
            val value = coin.valueInGold
            // Compute deposit amount using coin value, currency and exchange rates
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
        // Generate deposit description that will be displayed in the bank acoount
        // Distinguish singular/plural
        depositDesc = if (numCoins != 1) {
            "Deposited $numCoins coins"
        }
        else {
            "Deposited $numCoins coin"
        }
        // Add transaction to bank account history, update user score
        // (latter for leaderboard, same as balance of bank account)
        numCoinsDeposited += numCollectedCoins
        val newBalance = bankAccount.balance + amount
        userScore += amount
        Log.d(TAG,"[makeDeposit] New score is $userScore")
        val bankTransfer =  BankTransfer(getCurrentDate(),depositDesc,amount,newBalance,contents,true)
        bankAccount.balance = newBalance
        bankAccount.bankTransfers.add(bankTransfer)
        coinList.removeAll(contents)
        Log.d(TAG, "[makeDeposit] Deposit made\n" + bankTransfer.showDetails())
    }

    // Extract which coins were selected
    private fun getSelectedCoins() {
        selectedCoinList.clear() // Reset from previous
        for (coin in coinList) {
            if (coin.selected) {
                selectedCoinList.add(coin)
            }
        }
    }

    // Compute how many of selected coins were collected by user
    private fun getSelectedCollectedCoins() : Int {
        var numCollected = 0
        for (coin in selectedCoinList) {
            if (coin.collected) numCollected++
        }
        return numCollected
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

    // Contents of gift to be sent to other user
    private fun makeGiftContents(): ArrayList<Coin> {
        val giftContents : ArrayList<Coin>  = ArrayList()
        for (coin in selectedCoinList) {
            coin.selected = false
            coin.collected = false
            giftContents.add(coin)
        }
        return giftContents
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
        Log.d(TAG, "[loadData] Recalling data")
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
                // LOad username
                userName = task.result!!.getString(USERNAME_KEY)!!
                Log.d(TAG,"[loadData] Loaded username as $userName")
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

    // Update user's wallet and bank account Firestore
    private fun saveData() {
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

    // Display text summaries of wallet
    private fun generateSummary() {
        // Make text bits bold for clarity
        // Use italics to stress difference between collected and received coins
        val boldStyle = StyleSpan(Typeface.BOLD)
        val depositStateText = "Coins deposited today:"
        val depositStateMsg = "Coins deposited today: $numCoinsDeposited / $MAX_DEPOSIT"
        var spanStringBuilder =  SpannableStringBuilder(depositStateMsg)
        spanStringBuilder.setSpan(boldStyle,0,depositStateText.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        depositStateTextView.text = spanStringBuilder
        val numCoins = coinList.size
        if (numCoins != 0) {
            // Number of coins in the wallet
            val walletStateText = "Coins in the wallet:"
            val walletStateMsg = "Coins in the wallet: $numCoins / $MAX_COINS_LIMIT"
            spanStringBuilder = SpannableStringBuilder(walletStateMsg)
            spanStringBuilder.setSpan(boldStyle,0,walletStateText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            walletStateTextView.text = spanStringBuilder
            // Exchange rates
            val penyMsg = "PENY: ${String.format("%.2f",penyRate)}"
            spanStringBuilder = SpannableStringBuilder(penyMsg)
            spanStringBuilder.setSpan(boldStyle,0,4,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            penyRateTextView.text = spanStringBuilder
            val dolrMsg = "DOLR: ${String.format("%.2f",dolrRate)}"
            spanStringBuilder = SpannableStringBuilder(dolrMsg)
            spanStringBuilder.setSpan(boldStyle,0,4,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            dolrRateTextView.text = spanStringBuilder
            val quidMsg = "QUID: ${String.format("%.2f",quidRate)}"
            spanStringBuilder = SpannableStringBuilder(quidMsg)
            spanStringBuilder.setSpan(boldStyle,0,4,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            quidRateTextView.text = spanStringBuilder
            val shilMsg = "SHIL: ${String.format("%.2f",shilRate)}"
            spanStringBuilder = SpannableStringBuilder(shilMsg)
            spanStringBuilder.setSpan(boldStyle,0,4,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            shilRateTextView.text = spanStringBuilder
            // Description clarifying list of coins
            val italicStyle = StyleSpan(Typeface.BOLD_ITALIC)
            val coinListText = "Your coins: (collected ones normal (deposit limit applies), " +
                    "received ones italic (no deposit limit))"
            val italicStart = coinListText.indexOf("italic")
            spanStringBuilder = SpannableStringBuilder(coinListText)
            spanStringBuilder.setSpan(boldStyle,0,italicStart-1,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spanStringBuilder.setSpan(italicStyle,italicStart,coinListText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            coinListDesc.text = spanStringBuilder
            // Action buttons
            transferButton.visibility = View.VISIBLE
            depositButton.visibility = View.VISIBLE
            selectButton.visibility = View.VISIBLE
            deselectButton.visibility = View.VISIBLE
        }
        else {
            // Notify user that he has no coins
            val walletStateMsg = "You don't have any coins in the wallet! Collect coins on the" +
                    " map" + " or check for any unopened gifts."
            spanStringBuilder = SpannableStringBuilder(walletStateMsg)
            spanStringBuilder.setSpan(boldStyle,0,walletStateMsg.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            walletStateTextView.text = spanStringBuilder
            // Don't show exchange rates, action buttons or list of coins (no use of them in this case)
            ratesDesc.text = ""
            penyRateTextView.text=""
            dolrRateTextView.text=""
            quidRateTextView.text=""
            shilRateTextView.text=""
            coinListDesc.text = ""
            transferButton.visibility = View.INVISIBLE
            depositButton.visibility = View.INVISIBLE
            selectButton.visibility = View.INVISIBLE
            deselectButton.visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        // Start as usual if network connection available, otherwise sign out immediately
        if (isNetworkAvailable()) {
            Log.d(TAG,"[onStart] User connected, start as usual")
            // To keep track of when deposits made in case exchange rates expire
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
            loadData()
        }
        else {
            Log.d(TAG,"[onStart] User disconnected, sign out")
            Toast.makeText(this,"Can't communicate with server! Check your internet " +
                    "connection and log in again.", Toast.LENGTH_SHORT).show()
            signOut()
        }
    }

    // Check if internet connection is available
    private fun isNetworkAvailable() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    // Sign out user if no network connection available when activity launched
    private fun signOut() {
        Log.d(TAG,"[signOut] Signing out user")
        mAuth.signOut()
        val resetIntent = Intent(this, LoginActivity::class.java)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        resetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(resetIntent)
    }
}