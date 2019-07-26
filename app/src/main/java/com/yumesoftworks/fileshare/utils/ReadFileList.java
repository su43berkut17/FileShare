package com.yumesoftworks.fileshare.utils;

import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadFileList {
    private final static String TAG="ReadFileList";

    public ReadFileList(){
    }

    //public MutableLiveData<List<FileListEntry>> loadList(String path, Context context){
    public List<FileListEntry> loadList(String path){
        //mContext=context;

        File file=new File(path);
        Log.i(TAG,"we load the path: "+path);
        File[] list = file.listFiles();

        if (list!=null) {
            Log.i(TAG, "the list of files is " + file.listFiles());
            Log.i(TAG, "the number of files in the array is " + String.valueOf(list.length));

            //Final file list
            List<FileListEntry> fileList = new ArrayList<>();

            //temporary file List
            List<FileListEntry> tempFileList=new ArrayList<>();

            //we add the 1st level if it is not the upper level
            File upperLevel = new File(path);
            upperLevel = new File(upperLevel.getParent());

            //we check if parent folder is accessible
            //we add the item
            if (upperLevel != null && upperLevel.listFiles()!=null && upperLevel.getPath().equals("/storage")==false) {
                FileListEntry parentEntry = new FileListEntry(upperLevel.getAbsolutePath(),
                        "..",
                        0,
                        upperLevel.getParent(),
                        0,
                        null,
                        true);

                fileList.add(parentEntry);
            }

            //we add the directories only
            for (int i = 0; i < list.length; i++) {
                String name = list[i].getName();
                String absPath = list[i].getAbsolutePath();
                String parentPath = list[i].getParent();
                Boolean isDirectory = list[i].isDirectory();

                //we get the mime type
                Uri uri = Uri.fromFile(list[i]);

                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                        .toString());
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        fileExtension.toLowerCase());

                FileListEntry fileEntry = new FileListEntry(absPath, name, 0, parentPath, 0, mimeType, isDirectory);

                if (isDirectory) {
                    fileList.add(fileEntry);
                }else{
                    tempFileList.add(fileEntry);
                }
            }

            //sort both arrays
            Collections.sort(fileList);
            Collections.sort(tempFileList);

            //merge the arrays
            fileList.addAll(tempFileList);

            return fileList;
        }else{
            return null;
        }
    }
}
