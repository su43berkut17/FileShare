package com.yumesoftworks.fileshare.utils;

import android.arch.lifecycle.MutableLiveData;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadFileList {
    public ReadFileList(){

    }

    public MutableLiveData<List<FileListEntry>> loadList(String path){
        File file=new File(path);
        String[] list = file.list();

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
