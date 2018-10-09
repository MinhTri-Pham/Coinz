package com.example.minht.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

class Coin (val id: String, val currency: String, val position: LatLng) {


    override fun toString() : String = "Id: " + id + " Currency: " + currency + " Lat: " + position.latitude + " Lat: " + position.longitude

}