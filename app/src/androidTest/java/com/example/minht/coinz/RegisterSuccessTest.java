package com.example.minht.coinz;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.filters.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class RegisterSuccessTest {

    private String username = "";
    private String email = "";

    public String generateRandomUsername(int count) {
        String alphaNumSet = "abcdefghijklmnopqrstuvwxyz123456789";
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int pos = (int) (Math.random() * alphaNumSet.length());
            builder.append(alphaNumSet.charAt(pos));
        }
        return builder.toString();
    }

    @Before
    // Generate random alphanumeric username 3-8 characters long
    public void setUpCredentials() {
        Random random = new Random();
        int minLen = 3;
        int maxLen = 8;
        int count = random.nextInt((maxLen - minLen) + 1) + minLen; // Random int between 3-8 (inclusive)
        username = generateRandomUsername(count);
        email = username + "@test.com";
    }

    @Rule
    public ActivityTestRule<LoginActivity> mActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    //  Test successful registration
    @Test
    public void registerSuccessful() {
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
        appCompatEditText.perform(replaceText(username), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(withId(R.id.editTextEmailRegister));
        appCompatEditText2.perform(replaceText(email), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(withId(R.id.editTextPasswordRegister));
        String password = "123456";
        appCompatEditText3.perform(replaceText(password), closeSoftKeyboard());


        ViewInteraction appCompatButton = onView(withId(R.id.buttonRegister));
        appCompatButton.perform(click());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction loginButton = onView(withId(R.id.loginButton));
        ViewInteraction loginText = onView(withId(R.id.welcomeText));
        loginButton.check(matches(isDisplayed()));
        loginText.check(matches(isDisplayed()));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
