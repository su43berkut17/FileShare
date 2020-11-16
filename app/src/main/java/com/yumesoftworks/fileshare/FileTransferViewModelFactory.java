package com.yumesoftworks.fileshare;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class FileTransferViewModelFactory implements ViewModelProvider.Factory{
    private Application mApplication;
    private int mSendOrReceive;

    public FileTransferViewModelFactory(Application application){
        mApplication=application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new FileTransferViewModel(mApplication);
    }
}
