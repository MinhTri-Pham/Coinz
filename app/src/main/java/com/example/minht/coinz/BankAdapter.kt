package com.example.minht.coinz

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

    // Improve ListView performance using the ViewHolder pattern
    // See report Acknowledgements for more details
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view:View
        val holder: ViewHolder
        // Check if view already exists
        if (convertView == null) {
            // Inflate new view with subviews
            view = inflater.inflate(R.layout.bank_row,parent,false)
            holder = ViewHolder()
            holder.dateTextView = view.findViewById(R.id.txtDate)
            holder.descTextView = view.findViewById(R.id.txtDesc)
            holder.amountTextView = view.findViewById(R.id.txtAmount)
            holder.balanceTextView = view.findViewById(R.id.txtBalance)
            view.tag = holder // Use this view for future
        }
        else {
            // Skip inflation steps, get relevant subviews of row view immediately
            view = convertView
            holder = convertView.tag as ViewHolder
        }
        // Populate subviews
        val bankTransfer = getItem(position) as BankTransfer
        val dateTextView = holder.dateTextView
        val descTextView = holder.descTextView
        val amountTextView = holder.amountTextView
        val balanceTextView = holder.balanceTextView
        dateTextView.text = bankTransfer.date
        descTextView.text = bankTransfer.description
        amountTextView.text = String.format("%.2f",bankTransfer.amount)
        balanceTextView.text = String.format("%.2f",bankTransfer.balance)
        return view
    }

    // Stores row's subviews
    private class ViewHolder {
        lateinit var dateTextView : TextView
        lateinit var descTextView: TextView
        lateinit var amountTextView: TextView
        lateinit var balanceTextView: TextView
    }
}