package com.yumesoftworks.fileshare;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class JsonAvatarParserTest{
    //@Rule public JsonAvatarParser mAvatarParser=new JsonAvatarParser(InstrumentationRegistry.getContext());

    @Test
    public void test_connection(){
      JsonAvatarParser mAvatarParser=new JsonAvatarParser(InstrumentationRegistry.getContext());

        //we load the parser
        mAvatarParser.loadData();

        Thread.sleep(5000);

        AvatarAndVersion test=mAvatarParser.getAvatarAndVersionTest();

        //check data inside avatar an version
    }

    @Test
    public void TestParserMockData(){
        //mock json
        String mockJSON="";

        //we check if the json has been updated
        JsonAvatarParser mAvatarParser=new JsonAvatarParser(InstrumentationRegistry.getContext());

        AvatarAndVersion test=mAvatarParser.getAvatarAndVersionTest(mockJSON);

        //check data, it should be the same as the mock

    }
}