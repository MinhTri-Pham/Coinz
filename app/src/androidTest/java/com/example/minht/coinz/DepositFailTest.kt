package com.example.minht.coinz

import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.google.firebase.auth.FirebaseAuth

@LargeTest
@RunWith(AndroidJUnit4::class)
class DepositFailTest {

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
                Helper.prepareFullDeposit(uid!!)
            }
        }
    }
    // Test that already reaching daily limit for deposit won't allow further depositing
    @Test
    fun depositFail() {
        mActivityTestRule.launchActivity(null)
        try {
            Thread.sleep(8000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withContentDescription("Navigate up")).perform(click())
        onView(withText("Wallet")).perform(click())

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        // Check wallet summary before deposit
        val walletSizeText = onView(withId(R.id.wallet_state))
        walletSizeText.check(matches(withText("Coins in the wallet: 1 / 200")))

        val depositText = onView(withId(R.id.deposit_state))
        depositText.check(matches(withText("Coins deposited today: 25 / 25")))

        val coinCheckbox = onView(withId(R.id.coinCheckbox))
        coinCheckbox.perform(click())
        onView(withId(R.id.deposit_coins_button)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        // Check that coin stayed there and wallet summary didn't change
        coinCheckbox.check(matches(isDisplayed()))
        walletSizeText.check(matches(withText("Coins in the wallet: 1 / 200")))
        depositText.check(matches(withText("Coins deposited today: 25 / 25")))
    }
}
