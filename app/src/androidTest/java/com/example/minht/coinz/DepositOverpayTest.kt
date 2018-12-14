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
class DepositOverpayTest {

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
                Helper.prepareOverpayDeposit(uid!!)
            }
        }
    }
    // Test that if deposit goes over limit even when below limit before deposit, it won't go through
    @Test
    fun depositOverpayTest() {
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

        // Check wallet state before deposit
        val walletSizeText = onView(withId(R.id.wallet_state))
        walletSizeText.check(matches(withText("Coins in the wallet: 2 / 200")))

        val depositText = onView(withId(R.id.deposit_state))
        depositText.check(matches(withText("Coins deposited today: 24 / 25")))

        // Select all coins for deposit and check that wallet summary doesn't change
        onView(withId(R.id.select_all_button)).perform(click())
        onView(withId(R.id.deposit_coins_button)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        walletSizeText.check(matches(withText("Coins in the wallet: 2 / 200")))
        depositText.check(matches(withText("Coins deposited today: 24 / 25")))
    }
}
