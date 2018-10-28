package com.example.minht.coinz

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

class WalletActivity : AppCompatActivity() {

    private val tag = "WalletActivity" // For logging purposes
    private val preferencesFile = "MyPrefsFile"
    private var coinList : ArrayList<String> = ArrayList()
    private lateinit var viewAdapter : ArrayAdapter<String>
    private lateinit var coinListView : ListView
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        // Handle button clicks
        sendButton = findViewById(R.id.send_coins_button)
        sendButton.setOnClickListener {_ ->
            startActivity(Intent(this,SelectRecipientActivity::class.java))
        }
    }

    // Displays state of the wallet as a checkbox list of coins
    private fun displayCoins() {
        val walletStateTextView = findViewById<TextView>(R.id.wallet_state)
        val numCoins = coinList.size
        if (numCoins == 0) {
            val walletStateText = "Coins in the wallet: 0 / 1000 \n Collect coins on the map" +
                    " or check for any unopened gifts!"
            walletStateTextView.text = walletStateText
        } else {
            val walletStateText = "Coins in the wallet: $numCoins / 1000"
            walletStateTextView.text = walletStateText
            coinListView = findViewById(R.id.coins_checkable_list)
            coinListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            viewAdapter = ArrayAdapter(this,R.layout.row_layout,R.id.text_checkbox,coinList)
            coinListView.adapter = viewAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(tag,"[onStart] Recalling list of coins in the wallet")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val coinSet = settings.getStringSet("walletList", mutableSetOf())
        coinList.clear()
        coinList.addAll(coinSet)
        Log.d(tag,"[onStart] Displaying list of coins in the wallet")
        displayCoins()
    }

    override fun onStop() {
        super.onStop()
        Log.d(tag,"[onStop] Storing list of coins in the wallet")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putStringSet("walletList", coinList.toSet())
        editor.apply()
    }
}