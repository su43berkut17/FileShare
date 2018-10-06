package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

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

    public void saveData(UserInfoEntry userInfoEntry){
        new saveDatabaseAsyncTask(database).execute(userInfoEntry);
    }

    private static class saveDatabaseAsyncTask extends AsyncTask<UserInfoEntry,Void,Void>{
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
    }
}