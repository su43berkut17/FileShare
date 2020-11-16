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

        //Always sending 0 because while sending we want to display only unsent files and while
        //receiving we want to display all files nevertheless. Value is only modified
        //when we sent the files to successfully transferred
        data=database.fileListDao().loadFileBySentStatus(0);
    }

    public LiveData<List<FileListEntry>> getFileListInfo(){
        return data;
    }
}
