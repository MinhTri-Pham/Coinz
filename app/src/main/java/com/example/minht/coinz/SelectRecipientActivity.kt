package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SelectRecipientActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var gson : Gson
    private lateinit var selectedCoins : ArrayList<Coin> // Coins selected for transfer
    private lateinit var recipientCoins : ArrayList<Coin> // Coins owned by chosen recipient
    private lateinit var userCoins : ArrayList<Coin> // Coins owned by user
    private lateinit var usernameInputEditText : EditText
    private lateinit var confirmUsernameButton : Button

    // Constants
    companion object {
        const val TAG = "SelectRecipientActivity"
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY = "Username"
        const val WALLET_KEY = "Wallet"
        const val SELECTED_COINS_KEY="Selected coins" // For intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_recipient)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        gson = Gson()
        getSelectedCoins()
        usernameInputEditText = findViewById(R.id.recipient_username_editText)
        confirmUsernameButton = findViewById(R.id.confirm_username_button)
        // Handle button click
        confirmUsernameButton.setOnClickListener { _ -> makeTransfer() }
    }

    // Get coins chosen in previous screen
    private fun getSelectedCoins() {
        val selectedCoinsJSON = intent.extras.get(SELECTED_COINS_KEY).toString()
        val type = object : TypeToken<ArrayList<Coin>>(){}.type
        selectedCoins = gson.fromJson(selectedCoinsJSON, type)
    }

    // Find if typed username exists
    // If it does, update both user's and recipient's wallet in Firestore
    private fun makeTransfer() {
        val usernameText = usernameInputEditText.text.toString()
        if (!usernameText.isEmpty()) {
            // Query if input username exists in the database
            val usersRef = db.collection(COLLECTION_KEY)
            val findUsernameQuery = usersRef.whereEqualTo(USERNAME_KEY,usernameText)
            findUsernameQuery.get().addOnCompleteListener{ task: Task<QuerySnapshot> ->
                if (task.isSuccessful) {
                    if (!task.result!!.isEmpty) {
                        Log.d(TAG,"[findUsername] User found")
                        // Extract JSON representation
                        val recipientUId = task.result!!.documents.get(0).id
                        val recipientWalletString = task.result!!.documents.get(0).get(WALLET_KEY).toString()
                        val type = object : TypeToken<ArrayList<Coin>>(){}.type
                        recipientCoins = gson.fromJson(recipientWalletString, type)
                        recipientCoins.addAll(selectedCoins)
                        val recipientJson = gson.toJson(recipientCoins)
                        db.collection(COLLECTION_KEY).document(recipientUId).update(WALLET_KEY,recipientJson)
                        Log.d(TAG, "[makeTransfer] Updated wallet of recipient as $recipientJson")
                        userCoins.removeAll(selectedCoins)
                        val userJson = gson.toJson(userCoins)
                        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(WALLET_KEY,userJson)
                        Log.d(TAG, "[makeTransfer] Updated wallet of user as $userJson")
                        Toast.makeText(this, "Transfer completed", Toast.LENGTH_SHORT).show()

                    } else {
                        Log.d(TAG,"[makeTransfer] User not found")
                        Toast.makeText(this, "Such user doesn't exist, try a different username", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG,"[makeTransfer] Task failed")
                    Toast.makeText(this, "Query failed, check permissions", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            Log.d(TAG,"[findUsername] Can't query since input was empty")
            Toast.makeText(this, "Please choose a player to send coins to", Toast.LENGTH_SHORT).show()
        }
    }

    // Recall user's wallet from user's wallet
    override fun onStart() {
        super.onStart()
        Log.d(TAG,"[onStart] Recalling list of coins in the wallet")
        // Find JSON representation of user's wallet in FireStore
        val userDocRef = db.collection(COLLECTION_KEY).document(mAuth.uid!!)
        userDocRef.get().addOnCompleteListener{ task : Task<DocumentSnapshot> ->
            if (task.isSuccessful) {
                val walletString = task.result!!.get(WALLET_KEY).toString()
                Log.d(TAG,"[onStart] JSON representation of user's wallet $walletString")
                val type = object : TypeToken<ArrayList<Coin>>(){}.type
                userCoins = gson.fromJson(walletString,type)
                Log.d(TAG,"[onStart] Restored coins")

            } else {
                Log.d(TAG,"[onStart] Failed to extract JSON representation of user's wallet")
            }
        }
    }

    // Save user's wallet to Firestore
    /*override fun onStop() {
        super.onStop()
        val json = gson.toJson(userCoins)
        db.collection(COLLECTION_KEY).document(mAuth.uid!!).update(WALLET_KEY,json)
        Log.d(TAG, "[onStop] Stored user's wallet as $json")
    }*/


}
