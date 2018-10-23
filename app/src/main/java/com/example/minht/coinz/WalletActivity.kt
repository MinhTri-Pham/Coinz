package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

class WalletActivity : AppCompatActivity() {

    private var coinList = ArrayList<String>()
    private lateinit var viewAdapter : ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        // Get list of coins in the wallet
        coinList = intent.getStringArrayListExtra("coinsList")
        // Display state of the wallet
        val walletStateTextView = findViewById<TextView>(R.id.wallet_state)
        val numCoins = coinList.size
        if (numCoins == 0) {
            val walletStateText = "Coins in the wallet: 0 / 1000 \n Collect coins on the map" +
                    " or check for any unopened gifts!"
            walletStateTextView.text = walletStateText
        } else {
            val walletStateText = "Coins in the wallet: $numCoins / 1000"
            walletStateTextView.text = walletStateText
            viewAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,coinList)
            val coinListView = findViewById<ListView>(R.id.coins_list)
            coinListView.adapter = viewAdapter
        }

    }
}
