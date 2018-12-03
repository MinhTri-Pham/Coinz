package com.example.minht.coinz

import java.util.*

class BankTransfer (val date: String, val description: String, val amount: Double, val balance: Double, private val contents: ArrayList<Coin>, val isDeposit: Boolean) {

    // Shows date, currency counts and per coin contents
    fun showDetails() : String {
        val builder = StringBuilder()
        builder.append("Transaction date: $date\n\n")
        builder.append("Transaction summary:\n\n")
        if (isDeposit) {
            builder.append(generateSummary() + "\n\n")
            builder.append("Transaction details: \n\n")
            for (coin in contents) {
                builder.append(coin.toString()).append('\n')
            }
        }
        else {
            builder.append("${amount.toInt()} GOLD")
        }
        return builder.toString()
    }
    // Message displaying how many coins of each currency were in the transaction
    // Form: [A] PENY, [B] DOLR, [C] QUID, [D] SHIL, zero count currencies omitted
    private fun generateSummary() : String {
        var numPeny = 0
        var numDolr = 0
        var numQuid = 0
        var numShil = 0
        for (coin in contents) {
            val currency = coin.currency
            when(currency) {
                "PENY" -> numPeny++
                "DOLR" -> numDolr++
                "QUID" -> numQuid++
                "SHIL" -> numShil++
            }
        }
        // String representations for non-zero currency quantities.
        val quantities : MutableList<String> = mutableListOf()
        if (numPeny != 0) {
            quantities.add("$numPeny PENY")
        }
        if (numDolr != 0) {
            quantities.add("$numDolr DOLR")
        }
        if (numQuid != 0) {
            quantities.add("$numPeny QUID")
        }
        if (numShil != 0) {
            quantities.add("$numShil SHIL")
        }
        // Build final comma and space separated message from String quantities
        val separator = ", "
        val summaryBuilder = StringBuilder()
        for (quantity in quantities) {
            summaryBuilder.append(quantity).append(separator)
        }
        var summaryMsg = summaryBuilder.toString()
        // Remove separator after last quantity
        summaryMsg = summaryMsg.substring(0,summaryMsg.length - separator.length)
        return summaryMsg
    }

    // For logging purposes
    override fun toString(): String {
        return "Date: $date\n Description: $description\n Amount: $amount\n New balance:$balance"
    }
}