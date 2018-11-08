package com.yumesoftworks.fileshare;

import android.content.Context;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

@RunWith(AndroidJUnit4.class)
public class JsonAvatarParserTest{
    @Rule
    public ActivityTestRule<WelcomeScreenActivity> mActivityRule = new ActivityTestRule<>(WelcomeScreenActivity.class);

    @Test
    public void test_connection(){
        JsonAvatarParser mAvatarParser=new JsonAvatarParser(mActivityRule.getActivity().getApplicationContext());

        //we load the parser
        mAvatarParser.loadData();

        try {
            Thread.sleep(5000);
        }catch (Exception e){

        }

        AvatarAndVersion test=mAvatarParser.getAvatarAndVersionTest();

        //check data inside avatar an version

    }

    @Test
    public void TestParserMockData(){
        //mock json
        String mockJSON="";

        //we check if the json has been updated
        JsonAvatarParser mAvatarParser=new JsonAvatarParser(mActivityRule.getActivity().getApplicationContext());

        AvatarAndVersion test=mAvatarParser.getAvatarAndVersionTestMock(mockJSON);

        //check data, it should be the same as the mock

    }
}
