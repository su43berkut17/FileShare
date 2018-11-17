package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
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
        data=readFileList.loadList(path.getAbsolutePath(),this.getApplication().getApplicationContext());
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
        Log.d(TAG,"File Viewer View Model refresh data");
        ReadFileList s=new ReadFileList();
        //data=s.loadList(path,this.getApplication().getApplicationContext());
        //data.setValue(s.loadList(path,this.getApplication().getApplicationContext()).getValue());
        mPath.postValue(path);
        try{
            data.postValue(s.loadList(path, this.getApplication().getApplicationContext()).getValue());
        }catch (Exception e){
            Log.d(TAG,"error null");
        }
       // data.postValue(getData().getValue());
        //data=s.loadList("/storage/emulated/0",this.getApplication().getApplicationContext());
        //Log.d(TAG,"new data is updated, the length is "+data.getValue());

        //Log.d(TAG,"data is updated, the length is "+this.getData().getValue());
        //data.setValue(s.loadList("/storage/emulated/0",this.getApplication().getApplicationContext()).);
        //data.setValue(this.getData().);
    }

    public void saveFile(FileListEntry fileListEntry){
        new saveDatabaseAsyncTask(database).execute(fileListEntry);
    }

    private static class saveDatabaseAsyncTask extends AsyncTask<FileListEntry,Void,Void>{
        private AppDatabase database;

        saveDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final FileListEntry... params) {
            //we save the new file
            database.fileListDao().insertFile(params[0]);
            return null;
        }
    }

    public void deleteFile(FileListEntry fileListEntry){
        new deleteDatabaseAsyncTask(database).execute(fileListEntry);
    }

    private static class deleteDatabaseAsyncTask extends AsyncTask<FileListEntry, Void, Void>{
        private AppDatabase database;

        deleteDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(FileListEntry... fileListEntries) {
            Log.d(TAG,"Deleting "+fileListEntries[0].getFileName()+" is selected "+fileListEntries[0].getIsSelected());
            database.fileListDao().deleteFile(fileListEntries[0]);
            return null;
        }
    }

    public void deleteFileCheckbox(FileListEntry fileListEntry){
        new deletePathAsyncTask(database).execute(fileListEntry);
    }

    private static class deletePathAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        deletePathAsyncTask(AppDatabase recDatabase){database = recDatabase;}

        @Override
        protected Void doInBackground(FileListEntry... voids) {
            //we drop the table
            database.fileListDao().deleteFileNotSameId(voids[0].getPath());
            return null;
        }
    }

    public void deleteTable(){
        new deleteTableDatabaseAsyncTask(database).execute();
    }

    private static class deleteTableDatabaseAsyncTask extends AsyncTask<Void, Void, Void>{
        private AppDatabase database;

        deleteTableDatabaseAsyncTask(AppDatabase recDatabase){database = recDatabase;}

        @Override
        protected Void doInBackground(Void... voids) {
            //we drop the table
            database.fileListDao().clearFileList();
            return null;
        }
    }
}
