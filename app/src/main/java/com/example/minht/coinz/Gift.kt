package com.example.minht.coinz

import java.util.*

class Gift ( val date : String, val from: String, val contents: ArrayList<Coin>) {

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + date.hashCode()
        result = 31 * result + from.hashCode()
        result = 31 * result + contents.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Gift) return false
        if (date != other.date || from != other.from || contents.size != other.contents.size) return false
        for (coin in contents) {
            if (!other.contents.contains(coin)) return false
        }
        return true
    }

    fun shortDescription(): String{
        return "$date: $from sent you ${contents.size} coins!"
    }

    // Build newline separated message showing currency and value of each coin in the gift
    fun showContents(): String{
        val builder = StringBuilder()
        for (coin in contents) {
            builder.append(coin.toString()).append('\n')
        }
        return builder.toString()
    }
}
