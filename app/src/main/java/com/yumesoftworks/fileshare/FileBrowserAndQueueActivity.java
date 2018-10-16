package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;

//this activity will change depending if it is a tablet view
public class FileBrowserAndQueueActivity extends AppCompatActivity implements FileViewer.OnFragmentFileInteractionListener{
    private static final String TAG="FileBaQActivity";

    //2 panel
    private boolean mTwoPanel;

    //fragment parts
    private FileViewer fragmentFileViewer;

    //view model
    private FileViewerViewModel fileViewerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser_and_queue);

        //we check if it is 1 or 2 panels
        mTwoPanel=false;

        //we check for the permissions
        askForFilePermission();
    }

    private void initializeVariables(){
        //fragment stuff
        FragmentManager fragmentManager=getSupportFragmentManager();

        fragmentFileViewer=new FileViewer();

        //we load the files
        loadFragments(fragmentManager);
    }

    private void loadFragments(FragmentManager fragmentManager){
        fragmentManager.beginTransaction()
                .add(R.id.frag_afv_main, fragmentFileViewer)
                .commit();

        //we load the file list
        fileViewerViewModel = ViewModelProviders.of(this).get(FileViewerViewModel.class);
        fileViewerViewModel.getData().observe(this, fileViewerViewModelObserver);

        //refresh the data for the first time
        Log.i("FBAC","we will load the route: "+getFilesDir().getPath());
        //fileViewerViewModel.refreshData(getFilesDir().getPath());
        //fileViewerViewModel.refreshData(Environment.getExternalStorageDirectory().getPath());
    }

    final Observer<List<FileListEntry>> fileViewerViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we update the recyclerView Adapter
            Log.d(TAG,"ON CHANGED, the file list entries lenght returned in lifecycle is "+fileListEntries.size());
            fragmentFileViewer.updateFileRV(fileListEntries);
        }
    };

    private void askForFilePermission(){
        //we ask for permission before continuing
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //we create the fragments
                initializeVariables();
            } else {
                //we ask for permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            //we create the fragments
            initializeVariables();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //we create the fragment
            initializeVariables();
        }else{
            //go back to main activity
            Intent intent=new Intent(this,MainMenuActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onFragmentFileInteraction(FileListEntry fileListEntry) {
        //check if it is a directory
        if (fileListEntry.getDirectory()){
            //it is directory, we navigate to the new route
            Log.d(TAG,"the path to open is "+fileListEntry.getPath());
            fileViewerViewModel.refreshData(fileListEntry.getPath());

            //fileViewerViewModel.notify();

            //fragmentFileViewer.updateFileRV(fileViewerViewModel.getData().getValue());
        }else{
            //it is a file, depending on the checkbutton we decide if we save it or delete it
        }

    }
}
