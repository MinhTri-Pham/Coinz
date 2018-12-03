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
public class LoginFailTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    // Testing a few failing cases for logging in
    // Not filling any credentials stays in login
    @Test
    public void loginEmptyCredentialsTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton = onView(withId(R.id.loginButton));
        appCompatButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textView = onView(withId(R.id.welcomeText));
        textView.check(matches(isDisplayed()));
        appCompatButton.check(matches(isDisplayed()));

    }
    // Not filling password stays in login
    @Test
    public void loginEmptyPassword() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText = onView(withId(R.id.editTextEmail));
        appCompatEditText.perform(replaceText("test@email.com"), closeSoftKeyboard());
        appCompatEditText.perform(closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(withId(R.id.loginButton));
        appCompatButton.perform(click());

        ViewInteraction textView = onView(withId(R.id.welcomeText));
        textView.check(matches(isDisplayed()));
        appCompatButton.check(matches(isDisplayed()));
    }

    // Not filling password stays in login
    @Test
    public void loginEmptyEmail() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText = onView(withId(R.id.editTextPassword));
        appCompatEditText.perform(replaceText("123456"), closeSoftKeyboard());
        appCompatEditText.perform(closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(withId(R.id.loginButton));
        appCompatButton.perform(click());

        ViewInteraction textView = onView(withId(R.id.welcomeText));
        appCompatButton.check(matches(isDisplayed()));
        textView.check(matches(isDisplayed()));

    }

    // Test sign up login switches to register
    @Test
    public void switchToRegisterTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
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

        ViewInteraction textView = onView(withId(R.id.registerStart));
        textView.check(matches(isDisplayed()));

        ViewInteraction button = onView(withId(R.id.buttonRegister));
        button.check(matches(isDisplayed()));
    }

    // Test successful login

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