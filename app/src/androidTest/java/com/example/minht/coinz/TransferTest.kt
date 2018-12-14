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
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.Matchers

@LargeTest
@RunWith(AndroidJUnit4::class)
class TransferTest {

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

    // Test that sending coins to yourself is impossible
    // Test that successfully sending coin updates wallet accordingly
    @Test
    fun transferTest() {
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

        // Check wallet before transfer
        val walletSizeText = onView(withId(R.id.wallet_state))
        walletSizeText.check(matches(withText("Coins in the wallet: 1 / 200")))

        val depositText = onView(withId(R.id.deposit_state))
        depositText.check(matches(withText("Coins deposited today: 25 / 25")))

        // Check coin for transfer and try sending coins to yourself
        val coinCheckbox = onView(withId(R.id.coinCheckbox))
        coinCheckbox.perform(click())
        onView(withId(R.id.transfer_coins_button)).perform(click())
        val usernameInput = onView(withId(R.id.recipient_username_editText))
        usernameInput.perform(replaceText("espresso"))
        onView(withId(android.R.id.button1)).perform(click());

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        // Check that alert dialog still stays the same
        usernameInput.check(matches(withText("espresso")))

        // Send coin to one of test users
        usernameInput.perform(replaceText("stevie"))
        onView(withId(android.R.id.button1)).perform(click());

        try {
            Thread.sleep(5000)
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
        onView(withId(R.id.deposit_coins_button)).check(matches(Matchers.not(isDisplayed())))
        onView(withId(R.id.transfer_coins_button)).check(matches(Matchers.not(isDisplayed())))

        // Reset test recipient
        Helper.resetDocument("WLttejv3WjRyqjguVlrAcbCXpny1")
    }
}
