package com.example.minht.coinz


import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

@LargeTest
@RunWith(AndroidJUnit4::class)
class MapDownloadTest {

    companion object {
        const val PREFS_FILE = "MyPrefsFile"
        // Keys for Shared Preferences
        const val DOWNLOAD_DATE_KEY = "lastDownloadDate" // Date of map downloaded last
        const val MAP_KEY = "lastCoinMap" // Latest coin map
        const val PENY_KEY = "penyRate"
        const val DOLR_KEY = "dolrRate"
        const val QUID_KEY = "quidRate"
        const val SHIL_KEY = "shilRate"
    }

    private var penyRate = 0.0
    private var dolrRate = 0.0
    private var quidRate = 0.0
    private var shilRate = 0.0

    private fun getRates(testJson: JSONObject) {
        val rates = testJson.getJSONObject("rates")
        penyRate = rates.getDouble("PENY")
        dolrRate = rates.getDouble("DOLR")
        quidRate = rates.getDouble("QUID")
        shilRate = rates.getDouble("SHIL")
    }


    @Rule @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule @JvmField
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun setUp() {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithEmailAndPassword("espresso@test.com","123456").addOnCompleteListener{task ->
            if (task.isSuccessful) {
                val uid = mAuth.uid
                Helper.resetDocument(uid!!)
            }
        }
        val context = InstrumentationRegistry.getTargetContext()
        val settings = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val editor = settings.edit()
        // Make sure current date matches latest download date
        editor.putString(DOWNLOAD_DATE_KEY,Helper.getCurrentDate())
        val testIs = context.assets.open("test.geojson")
        val result = StringBuilder()
        // Build test map
        val reader = BufferedReader(InputStreamReader(testIs))
        var line = reader.readLine()
        while (line != null) {
            result.append(line)
            line = reader.readLine()
        }
        getRates(JSONObject(result.toString()))
        editor.putString(MAP_KEY,result.toString())
        editor.putString(PENY_KEY,penyRate.toString())
        editor.putString(DOLR_KEY,dolrRate.toString())
        editor.putString(QUID_KEY,quidRate.toString())
        editor.putString(SHIL_KEY,shilRate.toString())
        editor.apply()
    }

    @Test
    fun mapDownloadTest() {
        mActivityTestRule.launchActivity(null)
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        // Check info texts
        val progressText = onView(withId(R.id.progressInfo))
        progressText.check(matches(withText("Coins: 0 / 50")))
        val bonusText = onView(withId(R.id.bonusInfo))
        bonusText.check(matches(withText("Current bonus: 100 GOLD")))
        // Note map for 2018/12/16 opened all the time
        // Hence can't check map date info (since that is always set to current date in this case)
        // Check rates
        val penyText = onView(withId(R.id.penyInfo))
        penyText.check(matches(withText("PENY: 35.31")))
        val dolrText = onView(withId(R.id.dolrInfo))
        dolrText.check(matches(withText("DOLR: 28.65")))
        val quidText = onView(withId(R.id.quidInfo))
        quidText.check(matches(withText("QUID: 72.07")))
        val shilText = onView(withId(R.id.shilInfo))
        shilText.check(matches(withText("SHIL: 24.92")))

        val context = InstrumentationRegistry.getTargetContext()
        val settings = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val editor = settings.edit()
        // Overwrite Shared Preferences, this means map will have to be downloaded once in normal mode
        editor.putString(DOWNLOAD_DATE_KEY,"")
        editor.putString(MAP_KEY,"")
        editor.putString(PENY_KEY,"0.0")
        editor.putString(DOLR_KEY,"0.0")
        editor.putString(QUID_KEY,"0.0")
        editor.putString(SHIL_KEY,"0.0")
        editor.apply()
    }
}
