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
public class RegisterFailTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    // Test few failing scenarios for registration
    @Test
    public void registerFailTest() {
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

        // Too short password
        ViewInteraction appCompatEditText = onView(withId(R.id.editUsernameRegister));
        appCompatEditText.perform(replaceText("newUser"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(withId(R.id.editTextEmailRegister));
        appCompatEditText2.perform(replaceText("newUser@test.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(withId(R.id.editTextPasswordRegister));
        appCompatEditText3.perform(replaceText("12345"), closeSoftKeyboard());


        ViewInteraction appCompatButton = onView(withId(R.id.buttonRegister));
        appCompatButton.perform(click());

        ViewInteraction textView = onView(withId(R.id.registerStart));
        textView.check(matches(isDisplayed()));
        appCompatButton.check(matches(isDisplayed()));

        // Malformed email
        appCompatEditText2.perform(replaceText("newUsertest.com"), closeSoftKeyboard());
        appCompatEditText3.perform(replaceText("123456"), closeSoftKeyboard());
        appCompatButton.perform(click());
        textView.check(matches(isDisplayed()));
        appCompatButton.check(matches(isDisplayed()));
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
