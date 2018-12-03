package com.example.minht.coinz

 class Coin (var id: String, val currency: String, val valueInGold: Double,
             var selected: Boolean, var collected: Boolean) {

     // For equality
     override fun hashCode(): Int {
         var result = 17
         result = 31 * result + id.hashCode()
         result = 31 * result + currency.hashCode()
         result = 31 * result + valueInGold.hashCode()
         return result
     }

     // Equality based on id
     override fun equals(other: Any?): Boolean {
         if (other !is Coin) {
             return false
         } else {
             return id == other.id
         }
     }

     // For display/debugging purposes
     override fun toString() : String =  "$currency, Value: $valueInGold"
}