package com.yumesoftworks.fileshare.data;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.yumesoftworks.fileshare.ConstantValues;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FileListRepository {

    private static final String TAG="FileListRepository";
    private LiveData<List<FileListEntry>> data;
    private FileListDao fileListDao;
    private static AppDatabase database;

    public FileListRepository(Application application){
        //we load the database
        database=AppDatabase.getInstance(application);
        fileListDao=database.fileListDao();
        data=fileListDao.loadFileList();
    }

    public LiveData<List<FileListEntry>>getFiles(){
        return data;
    }

    public List<FileListEntry>getFilesDirect(){
        return fileListDao.loadFileListDirect();
    }

    //add file to the list
    public void saveFile(final FileListEntry fileListEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                database.fileListDao().insertFile(fileListEntry);
            }
        });
    }

    //add multiple files to the list
    public void saveFiles(final List<FileListEntry> listEntries){
        Executor myExecutor=Executors.newSingleThreadExecutor();

        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (FileListEntry uri:listEntries) {
                    if (!uri.getDirectory()) {
                        database.fileListDao().insertFile(uri);
                    }
                }
            }
        });
    }

    //update file
    public void updateFileSetTransferred(final FileListEntry fileListEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                fileListEntry.setIsTransferred(1);
                database.fileListDao().updateFile(fileListEntry);
            }
        });
    }

    //delete file
    public void deleteFile(final FileListEntry fileListEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT< ConstantValues.SAF_SDK){
                    database.fileListDao().deleteFile(fileListEntry);
                }else{
                    database.fileListDao().deleteFileNotSameId(fileListEntry.getPath());
                }

            }
        });
    }

    //delete with checkbox
    public void deleteFileCheckbox(final FileListEntry fileListEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //we delete the file checkbox
                Log.d(TAG,"Deleting "+fileListEntry.getFileName()+" is selected "+fileListEntry.getIsSelected());
                int retInt=database.fileListDao().deleteFileNotSameId(fileListEntry.getPath());
                Log.d(TAG,"retInt is "+retInt);
            }
        });
    }

    //delete a list of files
    public void deleteFiles(final List<FileListEntry> listEntries){
        Executor myExecutor=Executors.newSingleThreadExecutor();

        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (FileListEntry uri:listEntries) {
                    if (!uri.getDirectory()) {
                        database.fileListDao().deleteFile(uri);
                    }
                }
            }
        });
    }

    //clear the file list
    public void deleteTable(){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //we delete the file checkbox
                Log.d(TAG,"Deleting the table");
                database.fileListDao().clearFileList();
            }
        });
    }
}
