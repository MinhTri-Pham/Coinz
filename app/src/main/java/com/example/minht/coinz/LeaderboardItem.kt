package com.example.minht.coinz

class LeaderboardItem(val rank: Int, val username: String, val score: Double, val isUser : Boolean) {
    override fun toString(): String {
        val formattedScore = String.format("%.2f", score)
        if (isUser) {
            return "$rank. $username: $formattedScore (current user)"
        }
        else {
            return "$rank. $username: $formattedScore"
        }
    }
}