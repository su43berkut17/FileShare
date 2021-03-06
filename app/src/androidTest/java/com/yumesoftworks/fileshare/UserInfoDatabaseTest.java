package com.yumesoftworks.fileshare;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import android.content.Context;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListDao;
import com.yumesoftworks.fileshare.data.FileListEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class UserInfoDatabaseTest {
    private FileListDao mFileListDao;
    private AppDatabase mDb;

    @Before
    public void createDb(){
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mDb= Room.inMemoryDatabaseBuilder(context,AppDatabase.class).build();
        mFileListDao=mDb.fileListDao();
    }

    @After
    public void closeDb(){
        mDb.close();
    }

    @Test
    public void writeFileInfo(){
        //mock info
        int id=1;
        String path="TEST_USERNAME";
        String filename="TEST_FILENAME";
        int isTransferred=0;
        String parentFolder="TEST_PARENT_FOLDER";
        int isSelected=1;
        String mimeType="TEST MIME TYPE";

        FileListEntry fileListEntry=new FileListEntry(id,path,filename,isTransferred,parentFolder,isSelected,mimeType);

        mFileListDao.insertFile(fileListEntry);

        LiveData<List<FileListEntry>> readFileListEntryList=mFileListDao.loadFileList();
        try {
            wait(1000);
            if (readFileListEntryList.getValue().size() > 1) {
                FileListEntry readFileListEntry = readFileListEntryList.getValue().get(0);

                assertThat(readFileListEntry.getId(), equalTo(id));
                assertThat(readFileListEntry.getPath(), equalTo(path));
                assertThat(readFileListEntry.getFileName(), equalTo(filename));
                assertThat(readFileListEntry.getIsTransferred(), equalTo(isTransferred));
                assertThat(readFileListEntry.getParentFolder(), equalTo(parentFolder));
                assertThat(readFileListEntry.getIsSelected(), equalTo(isSelected));
                assertThat(readFileListEntry.getMimeType(), equalTo(mimeType));
            }
        }catch (Exception e){
            fail();
        }
    }
}
