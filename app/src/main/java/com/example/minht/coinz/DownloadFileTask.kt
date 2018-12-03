package com.example.minht.coinz

import android.os.AsyncTask
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Asynchronously downloads Geo-JSON map from server
class DownloadFileTask (private val caller : DownloadCompleteListener) : AsyncTask<String,Void,JSONObject>(){

    override fun doInBackground(vararg urls: String): JSONObject = try {
        loadFileFromNetwork(urls[0])
    } catch (e:IOException) {
        JSONObject()
    }

    // Gives a JSON object from the download URL
    private fun loadFileFromNetwork(urlString: String) : JSONObject {
        val stream : InputStream = downloadUrl(urlString)
        val result = StringBuilder()
        // Build data line by line
        val reader = BufferedReader(InputStreamReader(stream))
        var line = reader.readLine()
        while (line != null) {
            result.append(line)
            line = reader.readLine()
        }
        return JSONObject(result.toString())
    }

    // Given a string representation of a URL, sets up a connection and gets an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect() // Starts the query
        return conn.inputStream
    }

    // Notify when download complete
    override fun onPostExecute(result: JSONObject) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }
}