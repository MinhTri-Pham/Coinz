package com.example.minht.coinz

 class Coin (val id: String, val currency: String, val valueInGold : Double) {

     override fun equals(other: Any?): Boolean {
         if (other !is Coin) {
             return false
         } else {
             return id == other.id
         }
     }

     override fun toString() : String =  "$currency, Value: $valueInGold"

}