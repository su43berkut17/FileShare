package com.yumesoftworks.fileshare;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserInfoRepository;

import java.util.List;

public class TransferProgressActivityViewModel extends AndroidViewModel {
    private static String TAG="CombinedViewModel";

    //data
    private LiveData<List<UserInfoEntry>> data;

    //repository
    private UserInfoRepository repository;

    public TransferProgressActivityViewModel(Application application){
        super(application);

        repository=new UserInfoRepository(this.getApplication());

        data=repository.getTransferStatus();
    }

    public LiveData<List<UserInfoEntry>> getData(){
        data=repository.getTransferStatus();
        return data;
    }

    public void changeTransferStatus(int value){
        repository.switchTransfer(value);
    }

    public void changeServiceTypeStatus(int value){
        repository.switchServiceType(value);
    }

    public void setAsInactive(){
        repository.setAsInactive();
    }

}
