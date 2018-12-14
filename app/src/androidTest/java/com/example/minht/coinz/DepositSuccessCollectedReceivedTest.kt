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
import org.hamcrest.Matchers.not

@LargeTest
@RunWith(AndroidJUnit4::class)
class DepositSuccessCollectedReceivedTest {

    @Rule @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule @JvmField
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)!!

    @Before
    fun setUp() {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithEmailAndPassword("espresso@test.com","123456").addOnCompleteListener{task ->
            if (task.isSuccessful) {
                val uid = mAuth.uid
                Helper.prepareSuccessCollectedReceivedDeposit(uid!!)
            }
        }
    }
    // Test that depositing received coins is allowed as long as daily limit not broken with collected ones
    @Test
    fun depositCollectedTest() {
        mActivityTestRule.launchActivity(null)
        try {
            Thread.sleep(5000)
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

        // Check wallet summary before
        val walletSizeText = onView(withId(R.id.wallet_state))
        walletSizeText.check(matches(withText("Coins in the wallet: 2 / 200")))

        val depositText = onView(withId(R.id.deposit_state))
        depositText.check(matches(withText("Coins deposited today: 24 / 25")))

        // Select all coins in the wallet for deposit
        onView(withId(R.id.select_all_button)).perform(click())
        onView(withId(R.id.deposit_coins_button)).perform(click())

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        // Check wallet summary after deposit
        walletSizeText.check(matches(withText("You don't have any coins in the wallet! Collect coins on the" +
                " map" + " or check for any unopened gifts.")))
        depositText.check(matches(withText("Coins deposited today: 25 / 25")))
        // Check exchange rates and buttons hidden
        onView(withId(R.id.penyRateInfo)).check(matches(withText("")))
        onView(withId(R.id.dolrRateInfo)).check(matches(withText("")))
        onView(withId(R.id.quidRateInfo)).check(matches(withText("")))
        onView(withId(R.id.shilRateInfo)).check(matches(withText("")))
        onView(withId(R.id.deposit_coins_button)).check(matches(not(isDisplayed())))
        onView(withId(R.id.transfer_coins_button)).check(matches(not(isDisplayed())))
    }
}
