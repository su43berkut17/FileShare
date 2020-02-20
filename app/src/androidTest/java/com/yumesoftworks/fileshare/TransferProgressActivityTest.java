package com.yumesoftworks.fileshare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import androidx.test.espresso.matcher.ViewMatchers;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static junit.framework.TestCase.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TransferProgressActivityTest {
    /*@Rule
    public ActivityScenarioRule<TransferProgressActivity> activityScenarioRule
            = new ActivityScenarioRule<TransferProgressActivity>(TransferProgressActivity.class);*/

    @Test
    public void mainActivityAfterNoExtras() {
        ActivityScenario<TransferProgressActivity> activityScenario=ActivityScenario.launch(TransferProgressActivity.class);

        Log.d("-----------------------------------------","CHECKING IF VIEW IS VISIBLE");
        onView(withId(R.id.pb_atp_waitingForConnection)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        Log.d("-----------------------------------------","FINISHES CHECKING IF VIEW IS VISIBLE 1ST STEP");

        try {
            Thread.sleep(500);
            onView(withId(R.id.pb_atp_waitingForConnection)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        }catch (Exception e){
            fail();
        }
    }
}