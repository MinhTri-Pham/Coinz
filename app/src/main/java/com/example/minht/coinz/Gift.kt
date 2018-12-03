package com.example.minht.coinz

import java.util.*

class Gift ( val date : String, val from: String, val contents: ArrayList<Coin>) {

    // For equality
    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + date.hashCode()
        result = 31 * result + from.hashCode()
        result = 31 * result + contents.hashCode()
        return result
    }

    // Gifts equal if same date, sender and contents
    override fun equals(other: Any?): Boolean {
        if (other !is Gift) return false
        if (date != other.date || from != other.from || contents.size != other.contents.size) return false
        for (coin in contents) {
            if (!other.contents.contains(coin)) return false
        }
        return true
    }

    // Short message that will be displayed to the user
    fun shortDescription(): String {
        val numCoins = contents.size
        // Handle singular/plural case
        if (numCoins != 1) {
            return "$date: $from sent you ${contents.size} coins!"
        }
        return "$date: $from sent you 1 coin!"
    }

    // Build newline separated message showing currency and value of each coin in the gift
    // Shown when opening a gift
    fun showContents(): String{
        val builder = StringBuilder()
        for (coin in contents) {
            builder.append(coin.toString()).append('\n')
        }
        val string =  builder.toString()
        return string.substring(0,string.length-1) // Remove last newline character
    }
}