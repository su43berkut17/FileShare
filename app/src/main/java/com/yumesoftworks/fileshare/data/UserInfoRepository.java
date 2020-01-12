package com.yumesoftworks.fileshare.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.yumesoftworks.fileshare.TransferProgressActivity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


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
    public LiveData<List<UserInfoEntry>> getTransferStatus(){
        return data;
    }

    //switching the transfer status
    public void switchTransfer(final int transferValue){
        //we switch the transfer status to on or off
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
                userInfoEntry.setIsTransferInProgress(transferValue);
                database.userInfoDao().updateTask(userInfoEntry);
                Log.d(TAG,"The transfer file status activation is: "+transferValue);
            }
        });

        //new updateDatabaseAsyncTask(database).execute(transferValue);
    }

    public void switchServiceType(final int serviceTypeValue){
        Executor myExecutor=Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
                userInfoEntry.setTransferTypeSendOrReceive(serviceTypeValue);
                database.userInfoDao().updateTask(userInfoEntry);
                Log.d(TAG,"The transfer service type is: "+serviceTypeValue);
            }
        });
    }

    public void setAsInactive(){
        Executor myExecutor=Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
                userInfoEntry.setIsTransferInProgress(TransferProgressActivity.STATUS_TRANSFER_INACTIVE);
                userInfoEntry.setTransferTypeSendOrReceive(TransferProgressActivity.SERVICE_TYPE_INACTIVE);
                database.userInfoDao().updateTask(userInfoEntry);
                Log.d(TAG,"The transfer has been set as inactive");
            }
        });
    }

    /*private static class updateDatabaseAsyncTask extends AsyncTask<Integer,Void,Void> {
        private AppDatabase database;

        updateDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Integer... params) {
            UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
            userInfoEntry.setIsTransferInProgress(params[0]);
            database.userInfoDao().updateTask(userInfoEntry);
            Log.d(TAG,"The transfer file status activation is: "+params[0]);

            return null;
        }
    }*/

    //adding transfer numbers
    public void addSuccessfulTransferCounter(){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
                int currentCount=userInfoEntry.getNumberFilesTransferred();
                currentCount++;
                userInfoEntry.setNumberFilesTransferred(currentCount);
                database.userInfoDao().updateTask(userInfoEntry);
                Log.d(TAG,"We add a number more to the total transfers: "+currentCount);
            }
        });
        //new updateDatabaseCounterAsyncTask(database).execute();
    }

    //save transfer
    /*private static class updateDatabaseCounterAsyncTask extends AsyncTask<Void,Void,Void> {
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
    }*/
}
