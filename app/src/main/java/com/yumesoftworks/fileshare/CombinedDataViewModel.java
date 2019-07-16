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

    //directory file list
    private List<FileListEntry> dirFiles;

    //path
    private String path;

    //added files
    private List<FileListEntry> addedFiles;

    //repository
    private FileListRepository repository;

    public CombinedDataViewModel(Application application){
        super(application);

        repository=new FileListRepository(this.getApplication());
        data=repository.getFiles();

        /*//check if files has been initialized
        if (dirFiles==null) {
            //initialize the files to the default path
            //we set the data to be read
            ReadFileList readFileList = new ReadFileList();
            File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "");
            this.path=path.getPath();
            dirFiles = readFileList.loadList(path.getAbsolutePath(), this.getApplication().getApplicationContext());
        }

        //start comparing
        mergeData();*/
    }

    //update file list
    /*public void updateFileListPath(String recPath){
        ReadFileList readFileList = new ReadFileList();
        dirFiles=readFileList.loadList(recPath,this.getApplication().getApplicationContext());
        path=recPath;

        //compare
        mergeData();
    }

    //return path if needed
    public String getPath(){
        return path;
    }

    //merge data and update the final view model
    private void mergeData(){

        //we read the added files
        addedFiles=repository.getFilesPath(path).getValue();
        //addedFiles=repository.getFiles().getValue();
        //addedFiles=repository.getFilesDirect();

        //check if any is null
        if ((addedFiles==null)||(addedFiles.isEmpty())){
            Log.d(TAG,"No added files, it is empty ");
            //we return the normal file list
            //this catches for initialization of viewmodel in th emain activity where there is no file list
            if (dirFiles!=null && !dirFiles.isEmpty()) {
                data.setValue(dirFiles);
            }
        }else{
            Log.d(TAG,"The number of files is: "+addedFiles.size());
            //there is data in the added files so we compare and check the info
            for (FileListEntry fileElement:addedFiles) {
                //cycle for each saved element
                for (FileListEntry localDirectoryFile:dirFiles){
                    if (fileElement.getPath()==localDirectoryFile.getPath()) {
                        localDirectoryFile.setIsSelected(1);
                    }
                }
            }

            data.postValue(addedFiles);
            //data.notifyAll();
        }
    }*/

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
