package com.yumesoftworks.fileshare.data;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;

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

    //inserts
    public void saveFile(FileListEntry fileListEntry){
        new saveDatabaseAsyncTask(database).execute(fileListEntry);
    }

    private static class saveDatabaseAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
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

    //delete
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
            //we delete the file checkbox
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
