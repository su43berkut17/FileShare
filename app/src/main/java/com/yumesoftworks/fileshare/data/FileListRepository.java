package com.yumesoftworks.fileshare.data;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

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

    /*public LiveData<List<FileListEntry>>getFilesPath(String recPath){
        return fileListDao.loadFileListPath(recPath);
    }*/

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

        //new saveDatabaseAsyncTask(database).execute(fileListEntry);
    }

    /*private static class saveDatabaseAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
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
    }*/

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
        //new updateDatabaseSentAsyncTask(database).execute(fileListEntry);
    }

    /*private static class updateDatabaseSentAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        updateDatabaseSentAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final FileListEntry... params) {
            FileListEntry updateEntry=params[0];
            updateEntry.setIsTransferred(1);
            database.fileListDao().updateFile(updateEntry);
            return null;
        }
    }*/

    //delete file
    public void deleteFile(final FileListEntry fileListEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                database.fileListDao().deleteFile(fileListEntry);
            }
        });
        //new deleteDatabaseAsyncTask(database).execute(fileListEntry);
    }

    /*private static class deleteDatabaseAsyncTask extends AsyncTask<FileListEntry, Void, Void>{
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
    }*/

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
        //new deletePathAsyncTask(database).execute(fileListEntry);
    }

    /*private static class deletePathAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        deletePathAsyncTask(AppDatabase recDatabase){database = recDatabase;}

        @Override
        protected Void doInBackground(FileListEntry... voids) {
            //we delete the file checkbox
            Log.d(TAG,"Deleting "+voids[0].getFileName()+" is selected "+voids[0].getIsSelected());
            int retInt=database.fileListDao().deleteFileNotSameId(voids[0].getPath());
            Log.d(TAG,"retInt is "+retInt);
            return null;
        }
    }*/

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
        //new deleteTableDatabaseAsyncTask(database).execute();
    }

    /*private static class deleteTableDatabaseAsyncTask extends AsyncTask<Void, Void, Void>{
        private AppDatabase database;

        deleteTableDatabaseAsyncTask(AppDatabase recDatabase){database = recDatabase;}

        @Override
        protected Void doInBackground(Void... voids) {
            //we drop the table
            database.fileListDao().clearFileList();
            return null;
        }
    }*/
}
