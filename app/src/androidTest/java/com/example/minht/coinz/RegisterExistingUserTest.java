package com.example.minht.coinz;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RegisterExistingUserTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    // Can't register if user already exists
    @Test
    public void registerExistingUserTest() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatTextView = onView(withId(R.id.signUpLink));
        appCompatTextView.perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText = onView(withId(R.id.editUsernameRegister));
        appCompatEditText.perform(replaceText("ben1"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(withId(R.id.editTextEmailRegister));
        appCompatEditText2.perform(replaceText("ben1@test.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(withId(R.id.editTextPasswordRegister));
        appCompatEditText3.perform(replaceText("123456"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(withId(R.id.buttonRegister));
        appCompatButton.perform(click());

        ViewInteraction button = onView(withId(R.id.buttonRegister));
        button.check(matches(isDisplayed()));

        ViewInteraction textView = onView(withId(R.id.registerStart));
        textView.check(matches(isDisplayed()));

    }
}
