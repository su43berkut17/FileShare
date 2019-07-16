package com.yumesoftworks.fileshare;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.io.File;
import java.util.List;

public class FileViewerViewModel extends AndroidViewModel {
    private static MutableLiveData<List<FileListEntry>> data;
    private static MutableLiveData<String> mPath;
    private final static String TAG = "FileViewerViewModel";
    private AppDatabase database;

    public FileViewerViewModel(Application application){
        super(application);

        //we load the database
        database=AppDatabase.getInstance(this.getApplication());

        Log.d(TAG,"File Viewer View Model main constructor");

        //we set the data to be read
        ReadFileList readFileList=new ReadFileList();
        File path=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"");
        mPath=new MutableLiveData<>();
        mPath.postValue(path.getPath());
        //data=readFileList.loadList(path.getAbsolutePath(),this.getApplication().getApplicationContext());
    }

    public MutableLiveData<List<FileListEntry>> getData(){
        Log.d(TAG,"File Viewer View Model get data");
        return this.data;
    }

    public LiveData<String> getPath(){
        Log.d(TAG,"File Viewer View Model get data path");
        return this.mPath;
    }

    public void refreshData(String path){
        //lets check is the path is readable
        File tempFile=new File(path);
        if (tempFile.canRead()) {
            Log.d(TAG, "File Viewer View Model refresh data");
            ReadFileList s = new ReadFileList();

            mPath.postValue(path);
            try {
                //data.postValue(s.loadList(path, this.getApplication().getApplicationContext()).getValue());
            } catch (Exception e) {
                Log.d(TAG, "error null");
            }
        }
    }
}
