package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.io.File;
import java.util.List;

public class FileViewerViewModel extends AndroidViewModel {
    private MutableLiveData<List<FileListEntry>> data;
    private String TAG = "FileViewerViewModel";

    public FileViewerViewModel(Application application){
        super(application);

        //data= we set the data read from the route
        //refreshData();
        ReadFileList s=new ReadFileList();
        data=s.loadList("");
    }

    public LiveData<List<FileListEntry>> getData(){
        return data;
    }

    public void refreshData(String path){
        ReadFileList s=new ReadFileList();
        data=s.loadList(path);
    }
}
