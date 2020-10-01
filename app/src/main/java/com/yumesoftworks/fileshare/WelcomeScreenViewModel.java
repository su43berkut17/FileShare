package com.yumesoftworks.fileshare;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.util.Log;

import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserInfoRepository;

import java.util.List;

public class WelcomeScreenViewModel extends AndroidViewModel {
    private static final String TAG=WelcomeScreenViewModel.class.getSimpleName();

    private LiveData<List<UserInfoEntry>> userInfo;
    private UserInfoRepository repository;

    public WelcomeScreenViewModel(Application application){
        super(application);
        repository=new UserInfoRepository(this.getApplication());
        userInfo=repository.getUserData();
        Log.d(TAG,"retrieving tasks from view model");
    }

    public LiveData<List<UserInfoEntry>> getUserInfo(){

        if (userInfo==null){
            userInfo=repository.getUserData();
        }

        return userInfo;
    }

    public void saveData(final UserInfoEntry userInfoEntry){
        repository.saveData(userInfoEntry);
    }

    public void resetFlags(final UserInfoEntry userInfoEntry){
        repository.resetFlags(userInfoEntry);
    }

    public void switchandroid11SafWarning(boolean dialogViewed){
        repository.switchandroid11SafWarning(dialogViewed);
    }
}