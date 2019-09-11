package com.yumesoftworks.fileshare;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.FileListRepository;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.io.File;
import java.util.List;

public class CombinedDataViewModel extends AndroidViewModel {

    private static String TAG="CombinedViewModel";

    //combined data
    private LiveData<List<FileListEntry>> data;

    //repository
    private FileListRepository repository;

    public CombinedDataViewModel(Application application){
        super(application);

        repository=new FileListRepository(this.getApplication());
        data=repository.getFiles();
    }

    public void saveFile(FileListEntry file){
        repository.saveFile(file);
    }

    public void deleteFile(FileListEntry list){
        repository.deleteFile(list);
    }

    public void deleteFileCheckbox(FileListEntry list){
        repository.deleteFileCheckbox(list);
    }

    public void deleteTable(){
        repository.deleteTable();
    }

    public LiveData<List<FileListEntry>> getData(){
        return data;
    }
}
