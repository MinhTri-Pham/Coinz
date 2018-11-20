package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class BankActivity : AppCompatActivity() {
    // Display variables
    private lateinit var usernameSummary : TextView
    private lateinit var balanceSummary : TextView
    private lateinit var dateHeader : TextView
    private lateinit var descHeader : TextView
    private lateinit var amountHeader : TextView
    private lateinit var balanceHeader : TextView
    private lateinit var transferList : ListView
    private lateinit var bankAdapter : BankAdapter // Variable to display bank transfers
    // Bank account variable
    private lateinit var bankAccount: BankAccount
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
                usernameSummary.text = "Account owner: " + bankAccount.owner
                balanceSummary.text = "Account balance: " + String.format("%.2f",bankAccount.balance)
                bankAdapter = BankAdapter(this,bankAccount.bankTransfers)
                //mListView.addHeaderView(mListHeader)
                transferList.adapter = bankAdapter
            }
            else {
                Log.d(TAG,"[onStart] Failed to load bank account")
                Toast.makeText(this, "Failed to load your bank account, check your internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
