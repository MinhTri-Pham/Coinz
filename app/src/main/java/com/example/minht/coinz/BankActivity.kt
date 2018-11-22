package com.example.minht.coinz

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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class BankActivity : AppCompatActivity() {
    // Display variables
    private lateinit var usernameSummary : TextView
    private lateinit var balanceSummary : TextView
    private lateinit var transferDesc : TextView
    private lateinit var dateHeader : TextView
    private lateinit var descHeader : TextView
    private lateinit var amountHeader : TextView
    private lateinit var balanceHeader : TextView
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
        // Initialise header
        dateHeader = findViewById(R.id.date_header)
        dateHeader.text = "Date"
        descHeader = findViewById(R.id.desc_header)
        descHeader.text = "Description"
        amountHeader = findViewById(R.id.amount_header)
        amountHeader.text = "Amount"
        balanceHeader = findViewById(R.id.balance_header)
        balanceHeader.text = "Balance"
        transferList = findViewById(R.id.transfersList)
        // Initialise display period spinner using default items
        displayPeriodSpinner = findViewById(R.id.displayPeriodSpinner)
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
                val option = parent!!.getItemAtPosition(pos).toString()
                // Set display period accordingly and customise text just before listview
                when (option) {
                    "Today" -> {
                        displayPeriod = 1
                        transferDesc.text = "Transfers today:"
                    }
                    "Last week" -> {
                        displayPeriod = 7
                        transferDesc.text = "Transfers in last week:"
                    }
                    "Last 2 weeks" -> {
                        displayPeriod = 14
                        transferDesc.text = "Transfers in today:"
                    }
                    "Last month" -> {
                        val now = Date() // Current time
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                        val nowString = sdf.format(now).toString()
                        displayPeriod = nowString.substring(8).toInt()
                        Log.d(TAG,"Display period is $displayPeriod days")
                        transferDesc.text = "Transfers in last month:"
                    }
                }
                Log.d(TAG, "[initSpinner] $option selected as display period")
                filterTransfers()
                bankAdapter = BankAdapter(this@BankActivity,displayBankTransfers)
                transferList.adapter = bankAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(this@BankActivity, "Please select one option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Select transfer in the selected period
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
        // Load bank account from Firestore
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener{ task: Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val dataString = task.result!!.get(BANK_ACCOUNT_KEY).toString()
                Log.d(TAG, "[onStart] loaded bank account as $dataString")
                val gson = Gson()
                bankAccount = gson.fromJson(dataString, BankAccount::class.java)
                // Load views
                initSpinner()
                usernameSummary.text = "Account owner: " + bankAccount.owner
                balanceSummary.text = "Current balance: " + String.format("%.2f",bankAccount.balance)
            }
            else {
                Log.d(TAG,"[onStart] Failed to load bank account")
                Toast.makeText(this, "Failed to load your bank account, check your internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
