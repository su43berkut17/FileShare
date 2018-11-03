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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListRecyclerViewItemHelper;

import java.util.List;

//this activity will change depending if it is a tablet view
public class FileBrowserAndQueueActivity extends AppCompatActivity implements
        FileViewer.OnFragmentFileInteractionListener,
        FileViewer.OnButtonGoToQueueInterface,
        QueueViewer.QueueFragmentClickListener{
    private static final String TAG="FileBaQActivity";
    private static final int FILE_FRAGMENT=1000;
    private static final int QUEUE_FRAGMENT=1001;

    //2 panel
    private boolean mTwoPanel;
    private int mCurrentFragment;

    //fragment parts
    private FileViewer fragmentFileViewer;
    private QueueViewer fragmentQueueViewer;
    private FragmentManager fragmentManager;

    //view model
    private FileViewerViewModel fileViewerViewModel;
    private QueueViewerViewModel queueViewerViewModel;

    //for deletion in the queue viewer
    private boolean mIsNotDeletion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser_and_queue);

        //we check if it is 1 or 2 panels
        if (findViewById(R.id.frag_afv_queue) != null) {
            mTwoPanel = true;
        } else {
            mTwoPanel = false;
        }

        //we check for the permissions
        askForFilePermission();

        //we set the action bar
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initializeVariables(){
        //fragment stuff
        fragmentManager=getSupportFragmentManager();

        fragmentFileViewer=new FileViewer();
        fragmentQueueViewer=new QueueViewer();

        //we load the files
        loadFragments();
    }

    private void loadFragments(){
        fragmentManager.beginTransaction()
                .add(R.id.frag_afv_main, fragmentFileViewer)
                .commit();

        //we load the file list
        fileViewerViewModel = ViewModelProviders.of(this).get(FileViewerViewModel.class);
        fileViewerViewModel.getData().observe(this, fileViewerViewModelObserver);

        //we load the database file list for the observer for the file list
        queueViewerViewModel = ViewModelProviders.of(this).get(QueueViewerViewModel.class);

        //we set the current fragment
        mCurrentFragment=FILE_FRAGMENT;

        //we load the queue too if it is 2 panels
        if (mTwoPanel){
            fragmentManager.beginTransaction()
                    .replace(R.id.frag_afv_queue, fragmentQueueViewer)
                    .commit();

            //it should load automatically from the lifecycle
            queueViewerViewModel.getData().observe(this,queueViewerViewModelObserver);
        }

        changeActionBarName("FileShare - Send Files");
    }

    //observer for the file browser
    final Observer<List<FileListEntry>> fileViewerViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we update the recyclerView Adapter
            Log.d(TAG,"ON CHANGED, the file list entries length returned in lifecycle is "+fileListEntries.size());
            fragmentFileViewer.updateFileRV(fileListEntries);
        }
    };

    //observer for the queue viewer
    final Observer<List<FileListEntry>> queueViewerViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //only if it is not a swipe update since that is managed by the adapter
            if (mIsNotDeletion) {
                fragmentQueueViewer.updateQueue(fileListEntries);
            }else{
                mIsNotDeletion=true;
            }
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
        }else{
            //we check if it has been selected or not
            if (fileListEntry.getIsSelected()==0){
                //it is not selected so we delete it
                //we reset the is selected value as 1 so the fileListEntry is the same as the one that was saved before
                fileListEntry.setIsSelected(1);
                fileViewerViewModel.deleteFile(fileListEntry);
            }else{
                //it is selected so we save it
                fileViewerViewModel.saveFile(fileListEntry);
            }
        }
    }

    @Override
    public void onButtonQueueInteraction() {
        //we open the queue if it is single panel
        if (mTwoPanel!=true) {
            fragmentManager.beginTransaction()
                    .replace(R.id.frag_afv_main, fragmentQueueViewer)
                    .commit();

            //it should load automatically from the lifecycle
            queueViewerViewModel.getData().observe(this,queueViewerViewModelObserver);

            //we set the current fragment as the 1st one
            mCurrentFragment=QUEUE_FRAGMENT;

            changeActionBarName("FileShare - Queue");
        }

        //we reset the deletion
        mIsNotDeletion=true;
    }

    @Override
    public void onItemSwiped(FileListEntry file) {
        //we delete it from the database
        mIsNotDeletion=false;
        fileViewerViewModel.deleteFile(file);
    }

    @Override
    public void onButtonSendClicked() {
        //we go to the send activity
        //Intent intent=new Intent(this,SenderPickDestinationActivity.class);

        //this is a test to open directly the file progress
        Intent intent=new Intent(this,TransferProgressActivity.class);
        startActivity(intent);
    }

    //override the back button normal behaviour
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                //if we are in 1 panel mode
                if (mTwoPanel){
                    //we go back
                    onBackPressed();
                }else {
                    //we check the current fragment
                    if (mCurrentFragment == QUEUE_FRAGMENT) {
                        //we reload the  fragment 1
                        fragmentManager.beginTransaction()
                                .replace(R.id.frag_afv_main, fragmentFileViewer)
                                .commit();

                        //we set the current fragment
                        mCurrentFragment=FILE_FRAGMENT;
                        changeActionBarName("FileShare - Send Files");
                    } else {
                        //we are on the 1st fragment so we can go back
                        onBackPressed();
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //action bar
    //@Override
    public void changeActionBarName(String newTitle) {
        ActionBar titleUp=getSupportActionBar();
        titleUp.setTitle(newTitle);
    }
}
