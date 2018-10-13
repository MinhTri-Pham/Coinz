package com.example.minht.coinz

import android.os.AsyncTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DownloadFileTask (private val caller : DownloadCompleteListener) : AsyncTask<String,Void,String>(){

    override fun doInBackground(vararg urls: String): String = try {
        loadFileFromNetwork(urls[0])
    } catch (e:IOException) {
        "Unable to load content. Check your network connection"
    }

    private fun loadFileFromNetwork(urlString: String) : String {
        val stream : InputStream = downloadUrl(urlString)
        val result = StringBuilder()
        // Create a reader and read line by line
        val reader = BufferedReader(InputStreamReader(stream))
        var line = reader.readLine()
        while (line != null) {
            result.append(line)
            line = reader.readLine()
        }
        return result.toString()
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

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }
}