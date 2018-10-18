package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.util.List;

public class QueueViewerViewModel extends AndroidViewModel {

    private static LiveData<List<FileListEntry>> data;
    private String TAG = "QueueViewerViewModel";
    private AppDatabase database;

    public QueueViewerViewModel(@NonNull Application application) {
        super(application);

        //we load the database
        database=AppDatabase.getInstance(this.getApplication());

        Log.d(TAG,"Queue Viewer View Model main constructor");
        data=database.fileListDao().loadFileList();
    }

    public LiveData<List<FileListEntry>> getData(){
        return data;
    }
}
