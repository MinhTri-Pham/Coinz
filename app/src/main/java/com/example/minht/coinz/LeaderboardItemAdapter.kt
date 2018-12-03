package com.example.minht.coinz

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
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

    // Improve ListView performance using the ViewHolder pattern
    // See report Acknowledgements for more details
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view : View
        val holder: ViewHolder
        if (convertView == null) {
            // Check if view already exists
            view = inflater.inflate(R.layout.leaderboard_row, parent,false)
            holder = ViewHolder()
            holder.rankTextView = view.findViewById(R.id.rankLeaderItem) as TextView
            holder.usernameTextView = view.findViewById(R.id.usernameLeaderItem) as TextView
            holder.scoreTextView = view.findViewById(R.id.scoreLeaderItem) as TextView
            view.tag = holder
        }
        else {
            // Skip inflation steps, get relevant subviews of row view immediately
            view = convertView
            holder = convertView.tag as ViewHolder
        }
        // Populate subviews
        val rankTextView = holder.rankTextView
        val usernameTextView = holder.usernameTextView
        val scoreTextView = holder.scoreTextView
        val leaderboardItem = getItem(pos) as LeaderboardItem
        usernameTextView.text = leaderboardItem.username
        rankTextView.text = leaderboardItem.rank.toString()
        scoreTextView.text = String.format("%.2f",leaderboardItem.score)
        // Bold text and special text color/background if current user
        if (leaderboardItem.isUser) {
            rankTextView.setTypeface(null,Typeface.BOLD)
            rankTextView.setBackgroundColor(Color.parseColor("#a030559f"))
            rankTextView.setTextColor(Color.parseColor("#fafafa"))
            usernameTextView.setTypeface(null,Typeface.BOLD)
            usernameTextView.setBackgroundColor(Color.parseColor("#a030559f"))
            usernameTextView.setTextColor(Color.parseColor("#fafafa"))
            scoreTextView.setTypeface(null,Typeface.BOLD)
            scoreTextView.setBackgroundColor(Color.parseColor("#a030559f"))
            scoreTextView.setTextColor(Color.parseColor("#fafafa"))
        }
        return view
    }

    // Stores row's subviews
    private class ViewHolder {
        lateinit var rankTextView : TextView
        lateinit var usernameTextView : TextView
        lateinit var scoreTextView : TextView
    }
}