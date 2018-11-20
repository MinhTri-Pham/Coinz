package com.example.minht.coinz

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView

// Custom adapter to display coins in the wallet
class WalletAdapter(private val context: Context, private val dataSource: ArrayList<Coin>) : BaseAdapter() {

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(postition: Int): Any {
        return dataSource[postition]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val holder : ViewHolder
        if (view == null) {
            holder = ViewHolder()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.wallet_row,null,true)
            holder.coinCheckBox = view!!.findViewById(R.id.coinCheckbox) as CheckBox
            holder.coinSummary = view.findViewById(R.id.txtCoinSummary) as TextView
            view.tag = holder
        }
        else {
            holder = view.tag as ViewHolder
        }
        Log.d(TAG,"[getView] Make entry for coin")
        val coin = dataSource[position]
        holder.coinSummary!!.text = coin.toString()
        holder.coinCheckBox!!.isChecked = coin.selected
        holder.coinCheckBox!!.setTag(R.integer.btnplusview,view)
        holder.coinCheckBox!!.tag = position
        holder.coinCheckBox!!.setOnClickListener{_ : View ->
            val pos = holder.coinCheckBox!!.tag as Int
            Log.d(TAG, "[getView] Checkbox $pos clicked!")
            // Change status of Coin when checkbox clicked
            dataSource[pos].selected = !dataSource[pos].selected
            Log.d(TAG, "[getView] Reversed status of coin at $pos")

        }
        return view
    }

    private class ViewHolder {
        var coinCheckBox: CheckBox? = null
        var coinSummary: TextView? = null
    }

    companion object {
        const val TAG = "WalletAdapter"
        // lateinit var public_coinArrayList: ArrayList<Coin>
    }


}