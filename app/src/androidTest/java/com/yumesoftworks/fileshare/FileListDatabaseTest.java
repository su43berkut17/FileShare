package com.yumesoftworks.fileshare;

import androidx.room.Room;
import android.content.Context;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoDao;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class FileListDatabaseTest {
    private UserInfoDao mUserInfoDao;
    private AppDatabase mDb;

    @Before
    public void createDb(){
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mDb= Room.inMemoryDatabaseBuilder(context,AppDatabase.class).build();
        mUserInfoDao=mDb.userInfoDao();
    }

    @After
    public void closeDb(){
            mDb.close();
    }

    @Test
    public void writeUserInfo(){
        //mock info
        int id=1;
        String username="TEST_USERNAME";
        int avatar=1;
        int numberFilesTransferred=120;
        int assetVersion=1;
        int isTransferInProgress=0;

        UserInfoEntry userInfoEntry=new UserInfoEntry(id, username, avatar, numberFilesTransferred, assetVersion, isTransferInProgress);

        mUserInfoDao.insertTask(userInfoEntry);

        List<UserInfoEntry> readInfoEntryList=mUserInfoDao.loadUserWidget();
        UserInfoEntry readInfoEntry=readInfoEntryList.get(0);

        assertThat(readInfoEntry.getId(),equalTo(id));
        assertThat(readInfoEntry.getUsername(),equalTo(username));
        assertThat(readInfoEntry.getPickedAvatar(),equalTo(avatar));
        assertThat(readInfoEntry.getNumberFilesTransferred(),equalTo(numberFilesTransferred));
        assertThat(readInfoEntry.getAssetVersion(),equalTo(assetVersion));
        assertThat(readInfoEntry.getIsTransferInProgress(),equalTo(isTransferInProgress));
    }
}
