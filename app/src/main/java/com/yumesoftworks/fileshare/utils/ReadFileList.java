package com.yumesoftworks.fileshare.utils;

import android.arch.lifecycle.MutableLiveData;
import android.os.Environment;
import android.util.Log;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadFileList {
    private final static String TAG="ReadFileList";

    public ReadFileList(){

    }

    public MutableLiveData<List<FileListEntry>> loadList(String path){

        path=Environment.getExternalStorageDirectory().getPath();
        File file=new File(path);
        Log.i(TAG,"we load the path: "+path);
        String[] list = file.list();

        Log.i(TAG, "the number of files in the array is "+String.valueOf(list.length));

        MutableLiveData<List<FileListEntry>> LiveDataFileList=new MutableLiveData<>();
        List<FileListEntry> fileList=new ArrayList<>();

        for (int i=0;i<list.length;i++){
            File temp=new File(list[i]);

            String name=temp.getName();
            String absPath=temp.getAbsolutePath();
            FileListEntry fileEntry=new FileListEntry(absPath,name,0);

            fileList.add(fileEntry);
            LiveDataFileList.postValue(fileList);
        }

        return LiveDataFileList;
    }
}
