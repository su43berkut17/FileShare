package com.yumesoftworks.fileshare;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;

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
