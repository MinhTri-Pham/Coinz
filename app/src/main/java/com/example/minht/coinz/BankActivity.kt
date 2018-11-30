package com.example.minht.coinz

import android.content.Context
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
import android.view.View
import android.widget.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class BankActivity : AppCompatActivity() {
    // Display variables
    private lateinit var usernameSummary : TextView
    private lateinit var balanceSummary : TextView
    private lateinit var transferDesc : TextView
    private lateinit var transferList : ListView

    // Bank account and display period
    private lateinit var bankAdapter : BankAdapter // Variable to display bank transfers
    private lateinit var bankAccount: BankAccount
    private var displayPeriod = 7 // Number of days within which bank transfers are displayed
    private lateinit var displayPeriodSpinner : Spinner
    private var displayBankTransfers : ArrayList<BankTransfer> = ArrayList() // Bank transfers that are displayed
    // Other general variables for database
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        const val TAG = "BankActivity" // For logging purposes
        // For accessing data in Firestore
        const val COLLECTION_KEY = "Users"
        const val BANK_ACCOUNT_KEY = "Bank"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        usernameSummary = findViewById(R.id.usernameSummary)
        balanceSummary = findViewById(R.id.balanceSummary)
        transferDesc = findViewById(R.id.transfersDesc)
        displayPeriodSpinner = findViewById(R.id.displayPeriodSpinner)
        transferList = findViewById(R.id.transfersList)
        // Show info message with details of transaction
        transferList.setOnItemClickListener{_,_,pos,_ ->
            val selectedTransfer = displayBankTransfers[pos]
            val transferInfo = AlertDialog.Builder(this)
            transferInfo.setCancelable(true).setTitle("Transaction detail").setMessage(selectedTransfer.showDetails())
            transferInfo.show()
        }
    }

    // Initialise display period spinner
    private fun initSpinner() {
        val displayPeriodSpinnerAdapter = ArrayAdapter.createFromResource(this,R.array.displayPeriods,android.R.layout.simple_spinner_item)
        displayPeriodSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        displayPeriodSpinner.adapter = displayPeriodSpinnerAdapter
        // React to spinner item clicks
        displayPeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                displayBankTransfers.clear() // Clear from previous session (if any)
                if (isNetworkAvailable()) {
                    Log.d(TAG,"[initSpinner] User connected, rendering history as usual")
                    val option = parent!!.getItemAtPosition(pos).toString()
                    // Set display period accordingly and customise text just before listview
                    when (option) {
                        "Today" -> {
                            displayPeriod = 1
                            transferDesc.text = "Transactions today:"
                        }
                        "Last week" -> {
                            displayPeriod = 7
                            transferDesc.text = "Transactions in last week:"
                        }
                        "Last 2 weeks" -> {
                            displayPeriod = 14
                            transferDesc.text = "Transactions in last two weeks:"
                        }
                        "Last month" -> {
                            val now = Date() // Current time
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            val nowString = sdf.format(now).toString()
                            displayPeriod = nowString.substring(8).toInt()
                            Log.d(TAG,"Display period is $displayPeriod days")
                            transferDesc.text = "Transactions in last month:"
                        }
                    }
                    Log.d(TAG, "[initSpinner] $option selected as display period")
                    filterTransfers()
                }
                else {
                    Log.d(TAG,"[initSpinner] User disconnected, nothing to display")
                    Toast.makeText(this@BankActivity,"Can't load transactions! " +
                            "Check your internet connection", Toast.LENGTH_SHORT).show()
                }
                bankAdapter = BankAdapter(this@BankActivity,displayBankTransfers)
                transferList.adapter = bankAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(this@BankActivity, "Please select one option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Identify transactions in the selected period
    private fun filterTransfers() {
        val now = Date() // Current time
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        bankAccount.bankTransfers.reverse() // By construction, transfers are oldest to latest, so reverse
        for (bankTransfer in bankAccount.bankTransfers) {
            val dateString = bankTransfer.date
            val date = sdf.parse(dateString)
            val diffDays = (now.time - date.time) / (1000*60*60*24) // Count today as one day as well
            if (diffDays < displayPeriod) {
                displayBankTransfers.add(bankTransfer)
            }
            // If a bank transfer before period found, terminate
            // Since all other transfers will have later date than this one
            else {
                break
            }
        }
        bankAccount.bankTransfers.reverse() // Reset for future uses
    }

    override fun onStart() {
        super.onStart()
        if (isNetworkAvailable()) {
            // Load data if network connection available
            Log.d(TAG,"[onStart] User connected, start as usual")
            val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
            userDocRef.get().addOnCompleteListener{ task: Task<DocumentSnapshot> ->
                if (task.isSuccessful) {
                    val dataString = task.result!!.get(BANK_ACCOUNT_KEY).toString()
                    Log.d(TAG, "[onStart] loaded bank account as $dataString")
                    val gson = Gson()
                    bankAccount = gson.fromJson(dataString, BankAccount::class.java)
                    // Load views, make text parts bold
                    val usernameText = "Account owner:"
                    val balanceText = "Current balance:"
                    val usernameMsg = "Account owner: " + bankAccount.owner
                    val balanceMsg = "Current balance: " + String.format("%.2f",bankAccount.balance)
                    val boldStyle = StyleSpan(Typeface.BOLD)
                    var spanStringBuilder = SpannableStringBuilder(usernameMsg)
                    spanStringBuilder.setSpan(boldStyle,0,usernameText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    usernameSummary.text = spanStringBuilder
                    spanStringBuilder = SpannableStringBuilder(balanceMsg)
                    spanStringBuilder.setSpan(boldStyle,0,balanceText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    balanceSummary.text = spanStringBuilder
                    initSpinner()
                }
                else {
                    val message = task.exception!!.message
                    Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else {
            // Sign out user if no network connection
            Log.d(TAG,"[onStart] User disconnected, can't load data")
            signOut()
            Toast.makeText(this,"Can't communicate with server. Check your internet " +
                    "connection and log in again.", Toast.LENGTH_SHORT).show()
        }
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
