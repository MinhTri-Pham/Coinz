package com.example.minht.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

class Coin (val currency: String, val valueInGold : Double) {

    // For displaying in the wallet
    // Only approximate value shown for better user readibility
    override fun toString() : String =  " Currency: $currency Approximate value: " + String.format("%.3f",valueInGold)

}