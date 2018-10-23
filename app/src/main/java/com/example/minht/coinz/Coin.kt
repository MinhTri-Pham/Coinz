package com.example.minht.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

class Coin (val currency: String, val valueInGold : Double) {

    // For displaying in the wallet
    // Only approximate value shown for better user readability
    override fun toString() : String =  "$currency, Value: " + String.format("%.5f",valueInGold)

}