package com.example.minht.coinz

class BankTransfer (val date: String, val description: String, val amount: Double, val balance: Double) {

    // For displaying purposes
    override fun toString(): String {
        return "Date: $date\n Description: $description\n Amount: $amount\n New balance:$balance"
    }
}