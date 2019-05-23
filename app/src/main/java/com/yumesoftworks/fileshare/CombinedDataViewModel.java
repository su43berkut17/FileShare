package com.yumesoftworks.fileshare;

import android.app.Application;
import android.os.Environment;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.FileListRepository;
import com.yumesoftworks.fileshare.utils.ReadFileList;

import java.io.File;
import java.util.List;

public class CombinedDataViewModel extends AndroidViewModel {

    //combined data
    private MutableLiveData<List<FileListEntry>> data;

    //directory file list
    private List<FileListEntry> dirFiles;

    //added files
    private List<FileListEntry> addedFiles;

    //repository
    private FileListRepository repository;

    public CombinedDataViewModel(Application application){
        super(application);

        //check if files has been initialized
        if (dirFiles==null) {
            //initialize the files to the default path
            //we set the data to be read
            ReadFileList readFileList = new ReadFileList();
            File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "");
            dirFiles = readFileList.loadList(path.getAbsolutePath(), this.getApplication().getApplicationContext());
        }

        //start comparing
        mergeData();
    }

    //update file list
    public void updateFileList(String path){
        ReadFileList readFileList = new ReadFileList();
        dirFiles=readFileList.loadList(path,this.getApplication().getApplicationContext());

        //compare
        mergeData();
    }

    //merge data and update the final view model
    private void mergeData(){
        //check if any is null
        if ((addedFiles==null)||(addedFiles.isEmpty())){
            //we return the normal file list
            data.setValue(dirFiles);
        }else{
            //there is data in the added files so we compare and check the info
            for (FileListEntry fileElement:addedFiles) {
                //cycle for each saved element
                for (FileListEntry localDirectoryFile:dirFiles){
                    if (fileElement.getPath()==localDirectoryFile.getPath()) {
                        localDirectoryFile.setIsSelected(1);
                    }
                }
            }

            data.setValue(addedFiles);
        }
    }

    //TODO: update file database
    public void deleteFile(FileListEntry list){
        repository.deleteFile(list);
    }
}
