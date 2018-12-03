package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView

class StatsActivity : AppCompatActivity() {

    private var mapsCompleted = 0
    private var distWalked = 0.0
    private var calsBurned = 0.0

    private lateinit var mapsCompletedTextView : TextView
    private lateinit var distWalkedTextView: TextView
    private lateinit var calsBurnedTextView: TextView

    companion object {
        const val DIST_KEY = "Distance walked"
        const val CAL_KEY = "Calories burned"
        const val NUM_MAP_COMPLETED_KEY = "Number of completed maps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        mapsCompletedTextView = findViewById(R.id.mapsCompleted)
        distWalkedTextView = findViewById(R.id.distWalked)
        calsBurnedTextView = findViewById(R.id.calsBurned)
    }

    override fun onStart() {
        super.onStart()
        // Extract data passed from MainActivity
        mapsCompleted = intent.getIntExtra(NUM_MAP_COMPLETED_KEY,0)
        distWalked = intent.getDoubleExtra(DIST_KEY,0.0)
        calsBurned = intent.getDoubleExtra(CAL_KEY,0.0)
        // Display statistics, make text part bold for clarity
        val mapText = "Number of completed maps:"
        val distText = "Estimated distance walked:"
        val calText = "Estimated calories burned:"
        val mapMsg = "Number of completed maps: $mapsCompleted"
        var spanStringBuilder = SpannableStringBuilder(mapMsg)
        val boldStyle = StyleSpan(android.graphics.Typeface.BOLD)
        spanStringBuilder.setSpan(boldStyle,0,mapText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        mapsCompletedTextView.text = spanStringBuilder
        // If distance walked less than 1km, show in whole meters
        if (distWalked < 1000) {
            val distMsg = "Estimated distance walked: ${String.format("%.0f", distWalked)} m"
            spanStringBuilder = SpannableStringBuilder(distMsg)
            spanStringBuilder.setSpan(boldStyle,0,distText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            distWalkedTextView.text = spanStringBuilder
        }
        // Otherwise show in km with 1 decimal digit
        else {
            val distMsg = "Estimated distance walked: ${String.format("%.1f", distWalked/1000)} km"
            spanStringBuilder = SpannableStringBuilder(distMsg)
            spanStringBuilder.setSpan(boldStyle,0,distText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            distWalkedTextView.text = spanStringBuilder
        }
        // If calories burned less than 1kcal, show in whole calories
        if (calsBurned < 1000) {
            val calMsg = "Estimated calories burned: ${String.format("%.0f", calsBurned)} cal"
            spanStringBuilder = SpannableStringBuilder(calMsg)
            spanStringBuilder.setSpan(boldStyle,0,calText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            calsBurnedTextView.text = spanStringBuilder
        }
        // Otherwise show in kcal with 1 decimal digit
        else {
            val calMsg = "Estimated calories burned: ${String.format("%.1f", calsBurned/1000)} kcal"
            spanStringBuilder = SpannableStringBuilder(calMsg)
            spanStringBuilder.setSpan(boldStyle,0,calText.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            calsBurnedTextView.text = spanStringBuilder
        }
    }
}