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

        //path=Environment.getExternalStorageDirectory().getPath();
        //path="/storage/emulated/0/Download";
        File file=new File(path);
        Log.i(TAG,"we load the path: "+path);
        File[] list = file.listFiles();

        Log.i(TAG, "the number of files in the array is "+String.valueOf(list.length));
        //TODO: fix what happens if there are no files

        MutableLiveData<List<FileListEntry>> LiveDataFileList=new MutableLiveData<>();
        List<FileListEntry> fileList=new ArrayList<>();

        for (int j=0;j<list.length+1;j++){
            File temp;
            if (j==0){
                temp = new File(new File(list[0].getParent()).getParent());
            }else {
                temp = list[j-1];
            }

            String name=temp.getName();
            String absPath=temp.getAbsolutePath();
            String parentPath=temp.getParent();
            Boolean isDirectory=temp.isDirectory();

            //we get the mime type
            Uri uri = Uri.fromFile(temp);

            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());

            FileListEntry fileEntry=new FileListEntry(absPath,name,0,parentPath,0, mimeType, isDirectory);

            fileList.add(fileEntry);
            //LiveDataFileList.postValue(fileList);
        }

        LiveDataFileList.setValue(fileList);

        return LiveDataFileList;
    }
}
