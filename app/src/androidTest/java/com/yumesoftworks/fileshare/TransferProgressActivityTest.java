package com.yumesoftworks.fileshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.core.app.ApplicationProvider;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;


import static junit.framework.TestCase.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TransferProgressActivityTest {
    static Intent intent;
    static {
        intent = new Intent(ApplicationProvider.getApplicationContext(), TransferProgressActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_RECEIVING);
        bundle.putInt(TransferProgressActivity.LOCAL_PORT, 6554);
        intent.putExtras(bundle);
    }

    @Rule
    public ActivityScenarioRule<TransferProgressActivity> activityScenarioRule
            = new ActivityScenarioRule<>(intent);

    @Test
    public void mainActivityAfterNoExtras() {
        ActivityScenario<TransferProgressActivity> activityScenario=activityScenarioRule.getScenario();

        Log.d("-----------------------------------------","CHECKING IF VIEW IS VISIBLE");
        //onView(withId(R.id.pb_atp_waitingForConnection)).check(matches(isDisplayed()));
        onView(withId(R.id.pb_atp_waitingForConnection)).check(matches(not(isDisplayed())));
        //onView(withId(R.id.pb_atp_waitingForConnection)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Log.d("-----------------------------------------","FINISHES CHECKING IF VIEW IS VISIBLE 1ST STEP");

        /*try {
            Thread.sleep(500);
            activityScenario.moveToState(Lifecycle.State.RESUMED);
            onView(withId(R.id.pb_atp_waitingForConnection)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        }catch (Exception e){
            fail();
        }*/
    }
}