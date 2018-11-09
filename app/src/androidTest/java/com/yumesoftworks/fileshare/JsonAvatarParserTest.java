package com.yumesoftworks.fileshare;

import android.content.Context;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class JsonAvatarParserTest{
    @Rule
    public ActivityTestRule<WelcomeScreenActivity> mActivityRule = new ActivityTestRule<>(WelcomeScreenActivity.class);
    public Context mContext=InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void test_connection(){
        //JsonAvatarParser mAvatarParser=new JsonAvatarParser(mActivityRule.getActivity().getApplicationContext());
        JsonAvatarParser mAvatarParser=new JsonAvatarParser(mContext);

        //we load the parser
        mAvatarParser.loadData();

        try {
            Thread.sleep(5000);
        }catch (Exception e){

        }

        AvatarAndVersion test=mAvatarParser.getAvatarAndVersionTest();

        //we check if its loaded
        if (test==null){
            //if it didnt load it might mean there is no internet connection
            assertTrue(test!=null);
        }else{
            //we check the data that we received from the json
            assertTrue(test.getVersion()==1);

            assertTrue(test.getAvatarList().get(0).getPath()=="https://www.yumesoftworks.com/fileshare/avatars/avatar_9.png");
        }
    }

    @Test

    public void TestParserMockData(){
        //mock json
        String mockJSON="{\n" +
                "\t\"version\": 1,\n" +
                "\t\"items\":[\n" +
                "\t\t{\"url\":\"https://www.yumesoftworks.com/fileshare/avatars/avatar_9.png\"},\n" +
                "\t\t{\"url\":\"https://www.yumesoftworks.com/fileshare/avatars/avatar_10.png\"}\n" +
                "\t]\n" +
                "}";

        //we check if the json has been updated
        //JsonAvatarParser mAvatarParser=new JsonAvatarParser(mActivityRule.getActivity().getApplicationContext());
        JsonAvatarParser mAvatarParser=new JsonAvatarParser(mContext);

        AvatarAndVersion test=mAvatarParser.getAvatarAndVersionTestMock(mockJSON);

        //check data, it should be the same as the mock
        assertTrue(test.getVersion()==1);
        assertTrue(test.getAvatarList().get(0).getPath()=="https://www.yumesoftworks.com/fileshare/avatars/avatar_9.png");
    }
}
