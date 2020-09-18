package com.yumesoftworks.fileshare;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.StorageListEntry;
import com.yumesoftworks.fileshare.utils.ChangeShownPath;
import com.yumesoftworks.fileshare.utils.MergeFileListAndDatabase;
import com.yumesoftworks.fileshare.utils.UserConsent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//this activity will change depending if it is a tablet view
public class FileBrowserAndQueueActivity extends AppCompatActivity implements
        FileViewer.OnFragmentFileInteractionListener,
        FileViewer.OnButtonGoToQueueInterface,
        QueueViewer.QueueFragmentClickListener,
        UserConsent.UserConsentInterface{
    private static final String TAG="FileBaQActivity";
    private static final int FILE_FRAGMENT=1000;
    private static final int QUEUE_FRAGMENT=1001;
    private static final int LIVEDATA_UPDATE=2000;
    private static final int FILETREE_UPDATE=2001;

    private static final int FILE_PICK_CODE=5001;

    //fragment
    private int mCurrentFragment;
    private static final String CURRENT_FRAGMENT_TAG="currentFragmentTag";

    //admob
    private AdView mAdView;

    //fragment parts
    private FileViewer fragmentFileViewer;
    private QueueViewer fragmentQueueViewer;
    private FragmentManager fragmentManager;

    //view model
    private CombinedDataViewModel fileViewerViewModel;
    private CombinedDataViewModel queueViewerViewModel;
    private String mPath;
    private static final String CURRENT_PATH_TAG="CurrentPathTag";
    private Context thisActivity;

    //for deletion in the queue viewer
    private boolean mIsNotDeletion=true;

    //for checkbox interaction
    private boolean mAllowLivedataUpdate = true;

    //history
    private ArrayList<String> mFileHistory=new ArrayList<>();
    private static final String HISTORY_TAG="HistoryTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser_and_queue);

        thisActivity=this;

        //check the user consent
        UserConsent userConsent=new UserConsent(thisActivity);
        userConsent.checkConsent();

        if(savedInstanceState!=null){
            mCurrentFragment=savedInstanceState.getInt(CURRENT_FRAGMENT_TAG);
            mPath=savedInstanceState.getString(CURRENT_PATH_TAG);
            mFileHistory=savedInstanceState.getStringArrayList(HISTORY_TAG);
            Log.d(TAG,"Restoring the path: "+mPath);
        }else{
            mCurrentFragment=QUEUE_FRAGMENT;
            mPath=new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "").getAbsolutePath();
            mFileHistory.add(mPath);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.add_file_menu,menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_FRAGMENT_TAG,mCurrentFragment);
        outState.putString(CURRENT_PATH_TAG,mPath);
        outState.putStringArrayList(HISTORY_TAG,mFileHistory);
        Log.d(TAG,"Were saving the path: "+mPath);
    }

    private void initializeVariables(){
        Log.d(TAG,"initializing variables");
        //we check if we have old versions of the fragments
        fragmentManager=getSupportFragmentManager();

        //we need to check which fragment we will restore the instance of
        Log.d(TAG, "1 panel, we decide which one to load an which one to create");

        if (mCurrentFragment==FILE_FRAGMENT) {
            //for the file one
            Fragment fragmentFileViewerTemp = fragmentManager.findFragmentById(R.id.frag_afv_main);

            if (fragmentFileViewerTemp == null) {
                Log.d(TAG, "file fragment is null we create a new instance");
                fragmentFileViewer = new FileViewer();
            } else {
                Log.d(TAG, "fragment exists we take from fragment manager");
                fragmentFileViewer = (FileViewer) fragmentManager.findFragmentById(R.id.frag_afv_main);
            }

            //we create the queue one from scratch
            fragmentQueueViewer=new QueueViewer();
        }

        if (mCurrentFragment==QUEUE_FRAGMENT) {
            //queue one
            Fragment fragmentQueueViewerTemp = fragmentManager.findFragmentById(R.id.frag_afv_main);

            if (fragmentQueueViewerTemp == null) {
                Log.d(TAG, "queue fragment is null we create a new instance");
                fragmentQueueViewer = new QueueViewer();
            } else {
                Log.d(TAG, "queue fragment is null we create a new instance");
                fragmentQueueViewer = (QueueViewer) fragmentManager.findFragmentById(R.id.frag_afv_main);
            }

            //we create the file one from scratch only is if is api>21
            if (Build.VERSION.SDK_INT<ConstantValues.SAF_SDK) {
                fragmentFileViewer = new FileViewer();
            }
        }

        //we load the files
        loadFragments();
    }

    private void loadFragments(){
        Log.d(TAG,"Load fragments");
        //we create the viewmodel observers if they are null
        if (Build.VERSION.SDK_INT<ConstantValues.SAF_SDK) {
            if (fileViewerViewModel == null) {
                fileViewerViewModel = ViewModelProviders.of(this).get(CombinedDataViewModel.class);
            }
        }
        if (queueViewerViewModel==null) {
            queueViewerViewModel = ViewModelProviders.of(this).get(CombinedDataViewModel.class);
        }

        //we check which fragment to load depending on the current fragment
        if (mCurrentFragment==FILE_FRAGMENT) {
            fragmentManager.beginTransaction()
                    .replace(R.id.frag_afv_main, fragmentFileViewer)
                    .commit();

            //we set the current fragment
            mCurrentFragment=FILE_FRAGMENT;
        }

        //queue
        if (mCurrentFragment==QUEUE_FRAGMENT || mCurrentFragment==0){
            fragmentManager.beginTransaction()
                 .replace(R.id.frag_afv_main, fragmentQueueViewer)
                    .commit();
        }

        //we attach the observers to the activity
        if (Build.VERSION.SDK_INT<ConstantValues.SAF_SDK) {
            fileViewerViewModel.getData().observe(this, fileViewerViewModelObserver);
        }
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

        fragmentFileViewer.updatePath(new ChangeShownPath().filterString(mPath),mPath);
        fragmentFileViewer.updateFileRV(finalList);
    }

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
        if (Build.VERSION.SDK_INT >= ConstantValues.STORAGE_PERMISSION_SDK) {
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
            onBackPressed();
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

            //check if it is an upper level for history
            if (mFileHistory.get(mFileHistory.size()-1).contains(mPath)){
                mFileHistory.remove(mFileHistory.size()-1);
            }else{
                mFileHistory.add(mPath);
            }

            mAllowLivedataUpdate=true;
            mergeFileAndData(fileViewerViewModel.getData().getValue(),FILETREE_UPDATE);
        }else{
            Log.d(TAG,"saving changes");
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
        //backstack
        fragmentManager.popBackStack();
    }

    @Override
    public void fileFragmentRequestUpdate() {
        //we update the info of the fragment per the fragment request
        fragmentFileViewer.updateFileRV(fileViewerViewModel.getData().getValue());
        mergeFileAndData(null,FILETREE_UPDATE);
    }

    @Override
    public void fileFragmentSpinner(StorageListEntry entry) {
        //update path from spinner
        mFileHistory.clear();
        mPath=entry.getPath();
        mFileHistory.add(mPath);
        mAllowLivedataUpdate=true;
        mergeFileAndData(fileViewerViewModel.getData().getValue(),FILETREE_UPDATE);
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
        if (fileViewerViewModel!=null) {
            fileViewerViewModel.deleteFile(file);
        }else{
            queueViewerViewModel.deleteFile(file);
        }
    }

    @Override
    public void onButtonSendClicked() {
        //we go to the send activity
        Intent intent=new Intent(this, com.yumesoftworks.fileshare.SenderPickDestinationActivity.class);
        startActivity(intent);
    }

    //override the back button normal behaviour
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_add_files:
                //check if file browser or SAF
                if (Build.VERSION.SDK_INT<ConstantValues.SAF_SDK){
                    fragmentFileViewer=new FileViewer();

                    //we reload the  fragment
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.anim_enter_right, R.anim.anim_exit_left,R.anim.anim_enter_left,R.anim.anim_exit_right)
                            .replace(R.id.frag_afv_main, fragmentFileViewer)
                            .addToBackStack("null")
                            .commit();

                    mAllowLivedataUpdate = true;

                    mCurrentFragment=FILE_FRAGMENT;

                    //we update the data and path
                    fileFragmentRequestUpdate();
                }else{
                    //use SAF
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(i, FILE_PICK_CODE);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Build.VERSION.SDK_INT>ConstantValues.SAF_SDK) {
            if (requestCode == FILE_PICK_CODE && resultCode == Activity.RESULT_OK) {
                //check if 1 or multiple files
                //get result after user action (selecting files) and transform it into array of Uris
                if (data.getData() != null) { // only one uri was selected by user
                    //Get file name
                    //get the file name
                    DocumentFile file = DocumentFile.fromSingleUri(thisActivity, data.getData());
                    String name = file.getName();
                    String mime = file.getType();

                    FileListEntry fileEntry = new FileListEntry(data.getData().toString(), name, 0, "", 0, mime, false);
                    queueViewerViewModel.saveFile(fileEntry);
                    thisActivity.getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);

                } else if (data.getClipData() != null) {
                    int selectedCount = data.getClipData().getItemCount();
                    List<FileListEntry> listEntry = new ArrayList<>();

                    for (int i = 0; i < selectedCount; i++) {
                        DocumentFile file = DocumentFile.fromSingleUri(thisActivity, data.getClipData().getItemAt(i).getUri());
                        String name = file.getName();
                        String mime = file.getType();

                        FileListEntry fileEntry = new FileListEntry(data.getClipData().getItemAt(i).getUri().toString(), name, 0, "", 0, mime, false);
                        listEntry.add(fileEntry);
                        thisActivity.getContentResolver().takePersistableUriPermission(data.getClipData().getItemAt(i).getUri(),Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    queueViewerViewModel.saveFiles(listEntry);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        //we check the current fragment
        if (fragmentManager.getBackStackEntryCount()>0){
            //we are in browser
            if (mFileHistory.size()<=1) {
                //go back to the queue
                fragmentManager.popBackStack();
            }else {
                //we browse to the upper level
                mFileHistory.remove(mFileHistory.size()-1);
                mPath=mFileHistory.get(mFileHistory.size()-1);
                mAllowLivedataUpdate=true;
                mergeFileAndData(fileViewerViewModel.getData().getValue(),FILETREE_UPDATE);
            }
        }else{
            super.onBackPressed();
        }
    }

    //action bar
    public void changeActionBarName(String newTitle) {
        ActionBar titleUp=getSupportActionBar();
        titleUp.setTitle(newTitle);
    }

    @Override
    public void initAd(Boolean isTracking) {
        MobileAds.initialize(thisActivity,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_activity_file_browser_and_queue);
        AdRequest adRequest;

        if (isTracking){
            adRequest = new AdRequest.Builder().build();
        }else{
            Bundle extras = new Bundle();
            extras.putString("npa", "1");

            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class,extras)
                    .build();
        }

        mAdView.loadAd(adRequest);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isTracking);
    }
}