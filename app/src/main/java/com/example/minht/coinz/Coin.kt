package com.example.minht.coinz

 class Coin (val id: String, val currency: String, val valueInGold : Double) {

     override fun hashCode(): Int {
         var result = 17
         result = 31 * result + id.hashCode()
         result = 31 * result + currency.hashCode()
         result = 31 * result + valueInGold.hashCode()
         return result
     }

     override fun equals(other: Any?): Boolean {
         if (other !is Coin) {
             return false
         } else {
             return id.equals(other.id)
         }
     }

     override fun toString() : String =  "$currency, Value: $valueInGold\nId: $id"

}