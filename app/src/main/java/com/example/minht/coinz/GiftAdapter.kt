package com.example.minht.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class GiftAdapter (context: Context, private val dataSource: ArrayList<Gift>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(pos: Int): Any {
        return dataSource[pos]
    }

    override fun getItemId(pos: Int): Long {
        return pos.toLong()
    }

    // Improve ListView performance using the ViewHolder pattern
    // See report Acknowledgements for more details
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view : View
        val holder: ViewHolder
        if (convertView == null) {
            // Check if view already exists
            view = inflater.inflate(R.layout.gift_row, parent,false)
            holder = ViewHolder()
            holder.giftSummaryTextView = view.findViewById(R.id.giftSummary) as TextView
            view.tag = holder
        }
        else {
            // Skip inflation steps, get relevant subviews of row view immediately
            view = convertView
            holder = convertView.tag as ViewHolder
        }
        // Populate subviews
        val giftSummary = holder.giftSummaryTextView
        val gift = getItem(pos) as Gift
        giftSummary.text = gift.shortDescription()
        return view
    }
    // Stores row's subviews
    private class ViewHolder {
        lateinit var giftSummaryTextView : TextView
    }
}