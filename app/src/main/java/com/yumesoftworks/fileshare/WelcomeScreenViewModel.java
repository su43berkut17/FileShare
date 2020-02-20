package com.yumesoftworks.fileshare;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WelcomeScreenViewModel extends AndroidViewModel {
    private static final String TAG=WelcomeScreenViewModel.class.getSimpleName();

    private LiveData<List<UserInfoEntry>> userInfo;
    private AppDatabase database;

    public WelcomeScreenViewModel(Application application){
        super(application);
        database=AppDatabase.getInstance(this.getApplication());
        Log.d(TAG,"retrieving tasks from view model");
        userInfo=database.userInfoDao().loadUserInfo();
    }

    public LiveData<List<UserInfoEntry>> getUserInfo(){
        return userInfo;
    }

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

    /*private static class saveDatabaseAsyncTask extends AsyncTask<UserInfoEntry,Void,Void>{
        private AppDatabase database;

        saveDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final UserInfoEntry... params) {
            database.userInfoDao().emptyTable();
            database.userInfoDao().insertTask(params[0]);
            return null;
        }
    }*/
}