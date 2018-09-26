package com.yumesoftworks.fileshare;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

public class WelcomeScreenViewModel extends AndroidViewModel {
    private static final String TAG=WelcomeScreenViewModel.class.getSimpleName();

    private LiveData<List<UserInfoEntry>> userInfo;

    public WelcomeScreenViewModel(Application application){
        super(application);
        AppDatabase database=AppDatabase.getInstance(this.getApplication());
        Log.d(TAG,"retrieving tasks from view model");
        userInfo=database.userInfoDao().loadUserInfo();
    }

    public LiveData<List<UserInfoEntry>> getUserInfo(){
        return userInfo;
    }
}
