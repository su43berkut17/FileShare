package com.yumesoftworks.fileshare;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.MergeFileListAndDatabase;

import java.io.File;
import java.util.List;

//this activity will change depending if it is a tablet view
public class FileBrowserAndQueueActivity extends AppCompatActivity implements
        FileViewer.OnFragmentFileInteractionListener,
        FileViewer.OnButtonGoToQueueInterface,
        QueueViewer.QueueFragmentClickListener{
    private static final String TAG="FileBaQActivity";
    private static final int FILE_FRAGMENT=1000;
    private static final int QUEUE_FRAGMENT=1001;
    private static final int LIVEDATA_UPDATE=2000;
    private static final int FILETREE_UPDATE=2001;

    //2 panel
    private boolean mTwoPanel;
    private int mCurrentFragment;
    private static final String CURRENT_FRAGMENT_TAG="currentFragmentTag";

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    //fragment parts
    private FileViewer fragmentFileViewer;
    private QueueViewer fragmentQueueViewer;
    private FragmentManager fragmentManager;

    //view model
    //private FileViewerViewModel fileViewerViewModel;
    //private QueueViewerViewModel queueViewerViewModel;
    private CombinedDataViewModel fileViewerViewModel;
    private CombinedDataViewModel queueViewerViewModel;
    private String mPath;
    private static final String CURRENT_PATH_TAG="CurrentPathTag";

    //for deletion in the queue viewer
    private boolean mIsNotDeletion=true;
    //for checkbox interaction
    private boolean mAllowLivedataUpdate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser_and_queue);

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_activity_file_browser_and_queue);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if(savedInstanceState!=null){
            mCurrentFragment=savedInstanceState.getInt(CURRENT_FRAGMENT_TAG);
            mPath=savedInstanceState.getString(CURRENT_PATH_TAG);
        }else{
            mCurrentFragment=FILE_FRAGMENT;
            mPath=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "").getAbsolutePath();
        }

        //we check if it is 1 or 2 panels
        if (findViewById(R.id.frag_afv_queue) != null) {
            mTwoPanel = true;
        } else {
            mTwoPanel = false;
        }

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.afv_toolbar);
        setSupportActionBar(myToolbar);

        //we set the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //we check for the permissions
        askForFilePermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_FRAGMENT_TAG,mCurrentFragment);
        outState.putString(CURRENT_PATH_TAG,mPath);
    }

    private void initializeVariables(){
        Log.d(TAG,"initializing variables");
        //we check if we have old versions of the fragments
        fragmentManager=getSupportFragmentManager();

        int mainId=R.id.frag_afv_main;
        int queueId=R.id.frag_afv_queue;

        //we set the id vars
        if (mTwoPanel){
            //2 panels so we do different ids
            Log.d(TAG,"2 panel");
        }else{
            //reset the id for the queue
            queueId=R.id.frag_afv_main;
        }

        if (mTwoPanel) {
            //2 panels
            //we need to check which fragment we will restore the instance of
            Log.d(TAG, "2 panels");

            //for the file one
            Fragment fragmentFileViewerTemp = fragmentManager.findFragmentById(mainId);

            if (fragmentFileViewerTemp == null) {
                Log.d(TAG, "file fragment is null we create a new instance");
                fragmentFileViewer = new FileViewer();
            } else {
                Log.d(TAG, "fragment exists we take from fragment manager");
                fragmentFileViewer = (FileViewer) fragmentManager.findFragmentById(mainId);
            }

            //hide the file viewer button
            fragmentFileViewer.hideButton();

            //queue one
            Fragment fragmentQueueViewerTemp = fragmentManager.findFragmentById(queueId);

            if (fragmentQueueViewerTemp == null) {
                Log.d(TAG, "queue fragment is null we create a new instance");
                fragmentQueueViewer = new QueueViewer();
            } else {
                Log.d(TAG, "queue fragment is null we create a new instance");
                fragmentQueueViewer = (QueueViewer) fragmentManager.findFragmentById(queueId);
            }
        }else{
            //1 panel, check which fragment is active to be reloaded from the saved instance state
            //we need to check which fragment we will restore the instance of
            Log.d(TAG, "1 panel, we decide which one to load an which one to create");

            if (mCurrentFragment==FILE_FRAGMENT) {
                //for the file one
                Fragment fragmentFileViewerTemp = fragmentManager.findFragmentById(mainId);

                if (fragmentFileViewerTemp == null) {
                    Log.d(TAG, "file fragment is null we create a new instance");
                    fragmentFileViewer = new FileViewer();
                } else {
                    Log.d(TAG, "fragment exists we take from fragment manager");
                    fragmentFileViewer = (FileViewer) fragmentManager.findFragmentById(mainId);
                }

                //we create the queue one from scratch
                fragmentQueueViewer=new QueueViewer();
            }

            if (mCurrentFragment==QUEUE_FRAGMENT) {
                //queue one
                Fragment fragmentQueueViewerTemp = fragmentManager.findFragmentById(mainId);

                if (fragmentQueueViewerTemp == null) {
                    Log.d(TAG, "queue fragment is null we create a new instance");
                    fragmentQueueViewer = new QueueViewer();
                } else {
                    Log.d(TAG, "queue fragment is null we create a new instance");
                    fragmentQueueViewer = (QueueViewer) fragmentManager.findFragmentById(mainId);
                }

                //we create the file one from scratch
                fragmentFileViewer = new FileViewer();
            }
        }

        //we load the files
        loadFragments();
    }

    private void loadFragments(){
        Log.d(TAG,"Load fragments");
        //we create the viewmodel observers if they are null
        if (fileViewerViewModel==null){
            fileViewerViewModel = ViewModelProviders.of(this).get(CombinedDataViewModel.class);
        }
        if (queueViewerViewModel==null) {
            queueViewerViewModel = ViewModelProviders.of(this).get(CombinedDataViewModel.class);
        }

        //we check which fragment to load depending on the current fragment
        if (mCurrentFragment==0 || mCurrentFragment==FILE_FRAGMENT) {
            fragmentManager.beginTransaction()
                    .replace(R.id.frag_afv_main, fragmentFileViewer)
                    .commit();

            //we set the current fragment
            mCurrentFragment=FILE_FRAGMENT;
        }

        //queue
        if (mCurrentFragment==QUEUE_FRAGMENT || mTwoPanel){
            if (mTwoPanel){
                fragmentManager.beginTransaction()
                        .replace(R.id.frag_afv_queue, fragmentQueueViewer)
                        .commit();
            }else {
                fragmentManager.beginTransaction()
                        .replace(R.id.frag_afv_main, fragmentQueueViewer)
                        .commit();
            }
        }

        //we attach the observers to the activiy
        fileViewerViewModel.getData().observe(this,fileViewerViewModelObserver);
        //fileViewerViewModel.getPath().observe(this,fileViewerViewModelObserverPath);
        queueViewerViewModel.getData().observe(this,queueViewerViewModelObserver);

        changeActionBarName("FileShare - Send Files");
    }

    //observer for the file browser
    final Observer<List<FileListEntry>> fileViewerViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we update the recyclerView Adapter
            Log.d(TAG,"ON CHANGED, the file list entries length returned in lifecycle is "+fileListEntries.size());
            if (mAllowLivedataUpdate) {
                mergeFileAndData(fileListEntries,LIVEDATA_UPDATE);
            }
        }
    };

    //function that merges whether is from livedata update or from button interaction
    private void mergeFileAndData(List<FileListEntry> data, int type){
        //depending on the type call the merging

        List<FileListEntry> finalList;

        if (type==LIVEDATA_UPDATE){
            finalList=new MergeFileListAndDatabase().mergeFileListAndDatabase(data,mPath);
        }else{
            finalList=new MergeFileListAndDatabase().mergeFileListAndDatabase(fileViewerViewModel.getData().getValue(),mPath);
        }

        fragmentFileViewer.updateFileRV(finalList);
        fragmentFileViewer.updatePath(mPath);
    }

    final Observer<String> fileViewerViewModelObserverPath=new Observer<String>() {
        @Override
        public void onChanged(@Nullable String recPath) {
            //we update the path
            if (recPath!=null) {
                if (fragmentFileViewer.isResumed()) {
                    fragmentFileViewer.updatePath(recPath);
                }
            }
        }
    };

    //observer for the queue viewer
    final Observer<List<FileListEntry>> queueViewerViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //only if the queue fragment is attached
            if (fragmentQueueViewer.isAdded()) {
                //only if it is not a swipe update since that is managed by the adapter
                if (mIsNotDeletion) {
                    fragmentQueueViewer.updateQueue(fileListEntries);
                } else {
                    mIsNotDeletion = true;
                }
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
            Intent intent=new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        }
    }

    //when whe click on a file or directory
    @Override
    public void onFragmentFileInteraction(FileListEntry fileListEntry) {
        //check if it is a directory
        if (fileListEntry.getDirectory()){
            //it is directory, we navigate to the new route
            Log.d(TAG,"the path to open is "+fileListEntry.getPath());
            //fileViewerViewModel.refreshData(fileListEntry.getPath());
            //fileViewerViewModel.updateFileListPath(fileListEntry.getPath());
            mPath=fileListEntry.getPath();
            mAllowLivedataUpdate=true;
            mergeFileAndData(fileViewerViewModel.getData().getValue(),FILETREE_UPDATE);
        }else{
            //we check if it has been selected or not
            //mAllowLivedataUpdate=true;
            if (fileListEntry.getIsSelected()==0){
                //it is not selected so we delete it
                //we reset the is selected value as 1 so the fileListEntry is the same as the one that was saved before
                //fileListEntry.setIsSelected(1);
                //fileViewerViewModel.deleteFile(fileListEntry);
                mAllowLivedataUpdate =false;
                fileViewerViewModel.deleteFileCheckbox(fileListEntry);
            }else{
                //it is selected so we save it
                mAllowLivedataUpdate =false;
                fileViewerViewModel.saveFile(fileListEntry);
            }
        }
    }

    @Override
    public void onButtonQueueInteraction() {
        //we open the queue if it is single panel
        if (mTwoPanel!=true) {
            Log.d(TAG,"it is the queue button so we create a new instance of the queue");
            //we get or generate the queue
            //it is the queue one
            fragmentQueueViewer=new QueueViewer();

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
    public void fileFragmentRequestUpdate() {
        //we update the info of the fragment per the fragment request
        fragmentFileViewer.updateFileRV(fileViewerViewModel.getData().getValue());
        //fragmentFileViewer.updatePath(fileViewerViewModel.getPath());
        fragmentFileViewer.updatePath(mPath);
    }

    @Override
    public void queueFragmentRequestUpdate() {
        //we update the queue per the fragment request
        fragmentQueueViewer.updateQueue(queueViewerViewModel.getData().getValue());
    }

    @Override
    public void onItemSwiped(FileListEntry file, Boolean isLastSwipe) {
        //we delete it from the database
        mIsNotDeletion = isLastSwipe;

        fileViewerViewModel.deleteFile(file);
        if (mTwoPanel) {
            //fragmentFileViewer.updateFileRV(fileViewerViewModel.getData().getValue());
        }
    }

    @Override
    public void onButtonSendClicked() {
        //we go to the send activity
        Intent intent=new Intent(this, SenderPickDestinationActivity.class);
        startActivity(intent);

        //this is a test to open directly the file progress
        //Intent intent=new Intent(this,TransferProgressActivity.class);
        //startActivity(intent);
    }

    //override the back button normal behaviour
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        //if we are in 1 or 2 panel mode
        if (mTwoPanel){
            //we go back
            super.onBackPressed();
        }else {
            //we check the current fragment
            if (mCurrentFragment == QUEUE_FRAGMENT) {
                Log.d(TAG,"current is queue fragment so we reload the file fragment");

                //we reload the  fragment
                fragmentManager.beginTransaction()
                        .replace(R.id.frag_afv_main, fragmentFileViewer)
                        .commit();

                //we reattach the observer
                fileViewerViewModel.getData().observe(this, fileViewerViewModelObserver);
                //fileViewerViewModel.getPath().observe(this,fileViewerViewModelObserverPath);

                //we update the data and path
                fileFragmentRequestUpdate();

                //we set the current fragment
                mCurrentFragment=FILE_FRAGMENT;
                changeActionBarName("FileShare - Send Files");
            } else {
                //we are on the 1st fragment so we can go back
                super.onBackPressed();
            }
        }
    }

    //action bar
    public void changeActionBarName(String newTitle) {
        ActionBar titleUp=getSupportActionBar();
        titleUp.setTitle(newTitle);
    }
}