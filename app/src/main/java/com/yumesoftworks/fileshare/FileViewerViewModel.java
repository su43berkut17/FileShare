package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.io.File;
import java.util.List;

public class FileViewerViewModel extends AndroidViewModel {
    private static MutableLiveData<List<FileListEntry>> data;
    private String TAG = "FileViewerViewModel";

    public FileViewerViewModel(Application application){
        super(application);

        Log.d(TAG,"File Viewer View Model main constructor");
        //we set the data to be read
        ReadFileList readFileList=new ReadFileList();
        data=readFileList.loadList("/storage/emulated/0/Download",this.getApplication().getApplicationContext());
        //data=new MutableLiveData<List<FileListEntry>>();
    }

    public MutableLiveData<List<FileListEntry>> getData(){
        Log.d(TAG,"File Viewer View Model get data");
        return this.data;
    }

    public void refreshData(String path){
        Log.d(TAG,"File Viewer View Model refresh data");
        ReadFileList s=new ReadFileList();
        //data=s.loadList(path,this.getApplication().getApplicationContext());
        //data.setValue(s.loadList(path,this.getApplication().getApplicationContext()).getValue());
        data.postValue(s.loadList(path,this.getApplication().getApplicationContext()).getValue());
       // data.postValue(getData().getValue());
        //data=s.loadList("/storage/emulated/0",this.getApplication().getApplicationContext());
        //Log.d(TAG,"new data is updated, the length is "+data.getValue());

        //Log.d(TAG,"data is updated, the length is "+this.getData().getValue());
        //data.setValue(s.loadList("/storage/emulated/0",this.getApplication().getApplicationContext()).);
        //data.setValue(this.getData().);
    }
}
