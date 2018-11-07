package com.yumesoftworks.fileshare;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class JsonAvatarParserTest implements JsonAvatarParser.OnLoadedAvatars{
    private JsonAvatarParser mAvatarParser;

    @Test
    public void testJsonAvatarParser{
        mAvatarParser= new JsonAvatarParser(InstrumentationRegistry.getContext());

        //execute the object
        mAvatarParser.loadData();
    }

    @Override
    public void LoadedRemoteAvatars(AvatarAndVersion retAvatarAndVersion) {
        //we receive the avatar stuff

    }
}
