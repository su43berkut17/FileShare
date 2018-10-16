package com.yumesoftworks.fileshare.utils;

import android.arch.lifecycle.MutableLiveData;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadFileList {
    private final static String TAG="ReadFileList";
    private Context mContext;

    public ReadFileList(){

    }

    public MutableLiveData<List<FileListEntry>> loadList(String path, Context context){
        mContext=context;

        File file=new File(path);
        Log.i(TAG,"we load the path: "+path);
        File[] list = file.listFiles();

        Log.i(TAG, "the number of files in the array is "+String.valueOf(list.length));

        MutableLiveData<List<FileListEntry>> LiveDataFileList=new MutableLiveData<>();
        List<FileListEntry> fileList=new ArrayList<>();

        //we add the 1st level if it is not the upper level
        File upperLevel=new File(path);
        upperLevel=new File(upperLevel.getParent());

        //we add the item
        if (upperLevel != null) {
            FileListEntry parentEntry = new FileListEntry(upperLevel.getAbsolutePath(),
                    ".." ,
                    0,
                    upperLevel.getParent(),
                    0,
                    null,
                    true);
        }

        for (int i=0;i<list.length;i++){
            String name=list[i].getName();
            String absPath=list[i].getAbsolutePath();
            String parentPath=list[i].getParent();
            Boolean isDirectory=list[i].isDirectory();

            //we get the mime type
            Uri uri = Uri.fromFile(list[i]);

            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());

            FileListEntry fileEntry=new FileListEntry(absPath,name,0,parentPath,0, mimeType, isDirectory);

            fileList.add(fileEntry);
        }

        LiveDataFileList.setValue(fileList);

        return LiveDataFileList;
    }
}
