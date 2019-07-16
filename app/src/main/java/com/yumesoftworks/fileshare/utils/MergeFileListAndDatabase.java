package com.yumesoftworks.fileshare.utils;

import android.util.Log;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.util.List;

public class MergeFileListAndDatabase {
    private static final String TAG="MergeFiles";

    public List<FileListEntry> mergeFileListAndDatabase(List<FileListEntry> databaseList, String recPath){
        List<FileListEntry>addedFiles=databaseList;
        List<FileListEntry>dirFiles;

        ReadFileList readFileList = new ReadFileList();
        File path = new File(recPath, "");
        dirFiles = readFileList.loadList(path.getAbsolutePath());

        //check if any is null
        if ((addedFiles==null)||(addedFiles.isEmpty())){
            //Log.d(TAG,"No added files, it is empty ");
            //we return the normal file list without comparing since database is empty
            return dirFiles;
        }else{
            //Log.d(TAG,"The number of files is: "+addedFiles.size());
            //there is data in the added files so we compare and check the info
            for (FileListEntry fileElement:addedFiles) {
                //cycle for each saved element
                for (FileListEntry localDirectoryFile:dirFiles){
                    //if (fileElement.getPath()==localDirectoryFile.getPath()) {
                    if(fileElement.getPath().equals(localDirectoryFile.getPath())){
                        localDirectoryFile.setIsSelected(1);
                    }
                }
            }

            return dirFiles;
        }
    }
}