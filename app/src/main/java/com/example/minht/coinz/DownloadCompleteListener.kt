package com.example.minht.coinz

import org.json.JSONObject

interface DownloadCompleteListener {
    fun downloadComplete(result: JSONObject)
}