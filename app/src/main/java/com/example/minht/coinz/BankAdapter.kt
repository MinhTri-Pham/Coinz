package com.example.minht.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

// Custom adapter to display bank transfers
class BankAdapter(context: Context, private val dataSource: ArrayList<BankTransfer>) : BaseAdapter() {
    private val inflater : LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(postition: Int): Any {
        return dataSource[postition]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.bank_row, parent, false)
        val dateTextView = rowView.findViewById(R.id.txtDate) as TextView
        val descTextView = rowView.findViewById(R.id.txtDesc) as TextView
        val amountTextView = rowView.findViewById(R.id.txtAmount) as TextView
        val balanceTextView = rowView.findViewById(R.id.txtBalance) as TextView
        val bankTransfer = getItem(position) as BankTransfer
        dateTextView.text = bankTransfer.date
        descTextView.text = bankTransfer.description
        amountTextView.text = String.format("%.2f",bankTransfer.amount)
        balanceTextView.text = String.format("%.2f",bankTransfer.balance)
        return rowView
    }
}