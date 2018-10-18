package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.util.List;

public class FileViewerViewModel extends AndroidViewModel {
    private static MutableLiveData<List<FileListEntry>> data;
    private String TAG = "FileViewerViewModel";
    private AppDatabase database;

    public FileViewerViewModel(Application application){
        super(application);

        //we load the database
        database=AppDatabase.getInstance(this.getApplication());

        Log.d(TAG,"File Viewer View Model main constructor");

        //we set the data to be read
        ReadFileList readFileList=new ReadFileList();
        data=readFileList.loadList("/storage/emulated/0/Download",this.getApplication().getApplicationContext());
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
            //we delete the file
            database.fileListDao().deleteFile(fileListEntries[0]);
            return null;
        }
    }
}