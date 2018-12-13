package com.example.minht.coinz

class LeaderboardItem(val rank: Int, val username: String, val score: Double, val isUser : Boolean) {
    // For debugging purposes
    override fun toString(): String {
        val formattedScore = String.format("%.2f", score)
        return if (isUser) {
            "$rank. $username: $formattedScore (current user)"
        }
        else {
            "$rank. $username: $formattedScore"
        }
    }
}