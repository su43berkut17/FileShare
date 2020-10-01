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

    //reading user data
    public LiveData<List<UserInfoEntry>> getUserData(){
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
    }

    //switch service type
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

    //set transfer status as inactive
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

    //set android 11 warning as viewed
    public void switchandroid11SafWarning(boolean safWarningValue){
        Executor myExecutor=Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
                userInfoEntry.setAndroid11SafWarning(safWarningValue);
                database.userInfoDao().updateTask(userInfoEntry);
                Log.d(TAG,"The SAF dialog viewed value is: "+safWarningValue);
            }
        });
    }

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
    }

    //save data
    public void saveData(final UserInfoEntry userInfoEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                database.userInfoDao().emptyTable();
                database.userInfoDao().insertTask(userInfoEntry);
            }
        });
    }

    //reset flags
    public void resetFlags(final UserInfoEntry userInfoEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                database.fileListDao().clearFileList();
                database.userInfoDao().updateTask(userInfoEntry);
            }
        });
    }
}
