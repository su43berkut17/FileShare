package com.yumesoftworks.fileshare;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.ActivityTestRule;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4ClassRunner.class)
public class CreatingAvatarTest {
    @Rule
    public ActivityTestRule<WelcomeScreenActivity> mActivityRule = new ActivityTestRule<>(WelcomeScreenActivity.class);

    @Test
    public void saveChanges(){
        //select 1st avatar
        onView(withId(R.id.rv_aws_avatars)).perform(RecyclerViewActions.actionOnItemAtPosition(0,click()));

        //type the username

        //press the button

        //load database and check if it saved the 2 changes

    }
}
