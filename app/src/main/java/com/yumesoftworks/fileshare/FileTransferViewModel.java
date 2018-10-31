package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

public class FileTransferViewModel extends AndroidViewModel {
    private static LiveData<List<FileListEntry>> data;
    private String TAG = "FileTransferViewModel";
    private AppDatabase database;

    public FileTransferViewModel(@NonNull Application application) {
        super(application);

        //we load the database
        database=AppDatabase.getInstance(this.getApplication());

        data=database.fileListDao().loadFileList();
    }

    public LiveData<List<FileListEntry>> getFileListInfo(){
        return data;
    }
}
