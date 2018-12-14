package com.example.minht.coinz

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class Helper {
    companion object {
        const val TAG = "Helper"
        // Keys to access values in Firestore
        const val WALLET_KEY = "Wallet"
        const val BANK_KEY = "Bank"
        const val GIFT_KEY = "Gifts"
        const val NUM_DEPOSIT_KEY = "Number of deposited coins"
        const val SCORE_KEY = "Score"
        const val LAST_PLAY_KEY = "Last play date"
        const val VISITED_MARKERS_KEY = "Visited markers"
        const val NUM_COINS_KEY = "Number of collected coins"
        const val DIST_KEY = "Distance walked"
        const val CAL_KEY = "Calories burned"
        const val NUM_MAP_KEY = "Number of completed maps"
        const val DAILY_BONUS_KEY = "Daily bonus"

        fun getCurrentDate() : String {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val result = sdf.format(Date())
            Log.d(TAG,"Current date: $result")
            return result
        }

        fun resetDocument(uid: String) {
            val gson = Gson()
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("Users").document(uid)
            userDoc.update(WALLET_KEY,gson.toJson(ArrayList<Coin>()))
            userDoc.update(BANK_KEY,gson.toJson(BankAccount("espresso",0.0,ArrayList())))
            userDoc.update(GIFT_KEY,gson.toJson(ArrayList<Gift>()))
            userDoc.update(NUM_DEPOSIT_KEY,0)
            userDoc.update(SCORE_KEY,0.0)
            userDoc.update(LAST_PLAY_KEY,"")
            userDoc.update(VISITED_MARKERS_KEY,gson.toJson(mutableSetOf<String>()))
            userDoc.update(NUM_COINS_KEY,0)
            userDoc.update(DIST_KEY,0.0)
            userDoc.update(CAL_KEY,0.0)
            userDoc.update(NUM_MAP_KEY,0)
            userDoc.update(DAILY_BONUS_KEY,false)
            Log.d(TAG,"User doc reset")
        }
    }
}