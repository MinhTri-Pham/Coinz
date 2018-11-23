package com.example.minht.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class LeaderboardItemAdapter(context: Context, private val dataSource: ArrayList<LeaderboardItem>) : BaseAdapter() {

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

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view : View
        val holder: ViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.leaderboard_row, parent,false)
            holder = ViewHolder()
            holder.rankTextView = view.findViewById(R.id.rankLeaderItem) as TextView
            holder.usernameTextView = view.findViewById(R.id.usernameLeaderItem) as TextView
            holder.scoreTextView = view.findViewById(R.id.scoreLeaderItem) as TextView
            view.tag = holder
        }
        else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }
        val rankTextView = holder.rankTextView
        val usernameTextView = holder.usernameTextView
        val scoreTextView = holder.scoreTextView
        val leaderboardItem = getItem(pos) as LeaderboardItem
        rankTextView.text = leaderboardItem.rank.toString()
        usernameTextView.text = leaderboardItem.username
        scoreTextView.text = String.format("%.2f",leaderboardItem.score)
        return view
    }

    private class ViewHolder {
        lateinit var rankTextView : TextView
        lateinit var usernameTextView : TextView
        lateinit var scoreTextView : TextView
    }
}