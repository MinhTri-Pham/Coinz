package com.example.minht.coinz

import android.content.Context
import android.graphics.Typeface
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

    // Improve ListView performance using the ViewHolder pattern
    // See report Acknowledgements for more details
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val holder : ViewHolder
        if (view == null) {
            // Check if view already exists
            holder = ViewHolder()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.wallet_row,null,true)
            holder.coinCheckBox = view!!.findViewById(R.id.coinCheckbox) as CheckBox
            holder.coinSummary = view.findViewById(R.id.txtCoinSummary) as TextView
            view.tag = holder
        }
        else {
            // Skip inflation steps, get relevant subviews of row view immediately
            holder = view.tag as ViewHolder
        }
        // Populate subviews
        val coin = dataSource[position]
        Log.d(TAG,"[getView] Make entry for coin $coin")
        holder.coinSummary!!.text = coin.toString()
        // Mark coins received from other players in italic
        if (!coin.collected) {
            holder.coinSummary!!.setTypeface(null, Typeface.ITALIC)
        }
        else {
            holder.coinSummary!!.setTypeface(null, Typeface.NORMAL)
        }
        // Keep track of which entries are selected
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

    // Stores row's subviews
    private class ViewHolder {
        var coinCheckBox: CheckBox? = null
        var coinSummary: TextView? = null
    }

    companion object {
        const val TAG = "WalletAdapter"
        // lateinit var public_coinArrayList: ArrayList<Coin>
    }


}