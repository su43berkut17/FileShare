package com.yumesoftworks.fileshare.data;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.yumesoftworks.fileshare.ServiceFileShare;

import java.util.List;


public class UserInfoRepository {
    private static final String TAG="UserRepository";
    private LiveData<List<UserInfoEntry>> data;
    private UserInfoDao userInfoDao;
    private static AppDatabase database;

    public UserInfoRepository(Application application){
        //we load the database
        database=AppDatabase.getInstance(application);
        userInfoDao=database.userInfoDao();
        data=userInfoDao.loadUserInfo();
    }

    //reading transfer status
    public List<UserInfoEntry> getTransferStatus(){
        return userInfoDao.loadUserInfo().getValue();
    }

    //switching the transfer status
    public void switchTransfer(Boolean activateTransfer){
        //we switch the transfer status to on or off
        new updateDatabaseAsyncTask(database).execute(activateTransfer);
    }

    private static class updateDatabaseAsyncTask extends AsyncTask<Boolean,Void,Void> {
        private AppDatabase database;

        updateDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Boolean... params) {
            int status;
            //we read the status
            if (params[0]){
                status=1;
            }else{
                status=0;
            }

            UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
            userInfoEntry.setIsTransferInProgress(status);
            database.userInfoDao().updateTask(userInfoEntry);
            Log.d(TAG,"The transfer file status activation is: "+status);

            return null;
        }
    }

    //adding transfer numbers
    public void addSuccessfulTransferCounter(){
        new updateDatabaseCounterAsyncTask(database).execute();
    }

    //save transfer
    private static class updateDatabaseCounterAsyncTask extends AsyncTask<Void,Void,Void> {
        private AppDatabase database;

        updateDatabaseCounterAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
            int currentCount=userInfoEntry.getNumberFilesTransferred();
            currentCount++;
            userInfoEntry.setNumberFilesTransferred(currentCount);
            database.userInfoDao().updateTask(userInfoEntry);
            Log.d(TAG,"We add a number more to the total transfers: "+currentCount);

            return null;
        }
    }
}
