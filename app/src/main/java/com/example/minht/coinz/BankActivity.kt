package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ListView

class BankActivity : AppCompatActivity() {

    private lateinit var mListView : ListView
    private lateinit var mListHeader : View
    private lateinit var bankAdapter : BankAdapter // Variable to display bank transfers
    private var bankTransferList : ArrayList<BankTransfer> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        mListView = findViewById(R.id.transfersList)
        mListHeader = layoutInflater.inflate(R.layout.bank_header,null)
    }

    override fun onStart() {
        super.onStart()
        // Later: populate bank transfer list using JSON
        // Following snippet just for test purposes
        val test1 = BankTransfer("2018/11/12", "Added test1 transfer to the bank account", 25.123, 25.123)
        val test2 = BankTransfer("2018/11/10", "Added test2 transfer to the bank account", 32.514, 57.457)
        bankTransferList.add(test1)
        bankTransferList.add(test2)

        bankAdapter = BankAdapter(this,bankTransferList)
        mListView.addHeaderView(mListHeader)
        mListView.adapter = bankAdapter
    }
}
