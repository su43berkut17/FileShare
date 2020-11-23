package com.yumesoftworks.fileshare;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
