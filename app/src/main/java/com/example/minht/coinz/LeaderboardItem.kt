package com.example.minht.coinz

class LeaderboardItem(val rank: Int, val username: String, val score: Double) {
    override fun toString(): String {
        val formattedScore = String.format("%.2f", score)
        return "$rank. $username: $formattedScore"
    }
}