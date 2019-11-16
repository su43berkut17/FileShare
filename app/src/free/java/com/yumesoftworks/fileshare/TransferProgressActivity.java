package com.yumesoftworks.fileshare;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.FileListRepository;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserInfoRepository;

import java.util.ArrayList;
import java.util.List;

public class TransferProgressActivity extends AppCompatActivity implements
        FileTransferProgress.OnFragmentInteractionListener{

    private static final String TAG="TransferProgressAct";

    //extras names
    //general
    public static final String LOCAL_BROADCAST_REC="LocalBroadCastFileShare";
    public static final String EXTRA_TYPE_TRANSFER="ExtraType";
    public static final String LOCAL_IP="LocalIp";
    public static final String REMOTE_IP="RemoteIp";
    public static final String LOCAL_PORT="LocalPort";
    public static final String REMOTE_PORT="RemotePort";

    //type of message sent on text object
    public static final int TYPE_END=1001;
    public static final int TYPE_FILE_DETAILS=1002;
    public static final int TYPE_FILE_DETAILS_SUCCESS=1003;
    public static final int TYPE_FILE_TRANSFER_SUCCESS=1004;
    public static final int TYPE_FILE_TRANSFER_NO_SPACE=1005;

    //type of transfer statuses
    public static final int STATUS_TRANSFER_INACTIVE=3000;
    public static final int STATUS_TRANSFER_ACTIVE =3001;
    public static final int STATUS_TRANSFER_FINISHED =3002;
    public static final int STATUS_TRANSFER_SOCKET_ERROR =3003;
    public static final int STATUS_TRANSFER_OUT_OF_SPACE_ERROR =3004;
    public static final int STATUS_TRANSFER_NOTIFICATION_CANCEL =3005;

    //broadcast actions
    public static final String ACTION_UPDATE_UI="updateUI";

    //name of bundle objects coming from service
    public static final String ACTION_UPDATE_UI_DATA="updateUIData";
    private static final String SERVICE_STARTED_STATUS="serviceStatusInitial";

    private LinearLayout mProgressBarHide;

    //this activity context
    private Context thisActivity;

    //constants for the actions
    public static final int FILES_SENDING=2001;
    public static final int FILES_RECEIVING=2002;
    public static final int RELAUNCH_APP=2003;
    public static final String ACTION_SERVICE ="ReceivingFiles";

    //fragment parts
    private FileTransferProgress fragmentFileTransferProgress;
    private FragmentManager fragmentManager;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;

    //viewmodel
    private FileTransferViewModel fileTransferViewModel;
    private TransferProgressActivityViewModel transferProgressActivityViewModel;

    //Dialog
    private AlertDialog mGeneralDialog;

    //Ads
    private AdView mAdView;

    //type
    private int typeOfService;
    private int mHasServiceStarted;
    private boolean mIsServiceBound=false;

    //service binding
    private ServiceFileShare mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        //we heck if the service was started once
        if (savedInstanceState!=null){
            mHasServiceStarted = savedInstanceState.getInt(SERVICE_STARTED_STATUS);
            Log.e(TAG,"we have saved the value which is "+mHasServiceStarted);
        }else{
            Log.e(TAG,"we havent saved an instance yet, service started set to 0");
            mHasServiceStarted=0;
        }

        Log.d(TAG,"has the service started for the 1st time: "+mHasServiceStarted);

        thisActivity=this;

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_transfer_progress);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //we get the instance of the indeterminate progress bar
        mProgressBarHide=findViewById(R.id.pb_atp_waitingForConnection);

        if(mHasServiceStarted==0) {
            //we check the intent with the information to start the service
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();

            //get the bundle with the extras to start the service
            try {
                //get the data to see how do we start the service
                typeOfService = extras.getInt(EXTRA_TYPE_TRANSFER);
                Log.d(TAG, "The extras are: " + typeOfService);
            } catch (Exception e) {
                Log.d(TAG, "There are no extras");
                typeOfService = RELAUNCH_APP;
            }

            if (typeOfService != RELAUNCH_APP) {
                //first initialization
                //choose data in the intent
                if (typeOfService == FILES_SENDING) {
                    //we start the services as sending stuff
                    extras.putInt(ACTION_SERVICE, FILES_SENDING);
                } else if (typeOfService == FILES_RECEIVING) {
                    //we start the service as receiving stuff
                    extras.putInt(ACTION_SERVICE, FILES_RECEIVING);
                }

                //start the service
                //service intent
                Intent serviceIntent = new Intent(this, ServiceFileShare.class);
                serviceIntent.putExtras(extras);
                mHasServiceStarted=1;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Log.d(TAG, "starting on create the service 1st");
                        startForegroundService(serviceIntent);
                    } else {
                        Log.d(TAG, "starting on create the service 1st");
                        startService(serviceIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Couldnt start service " + e.getMessage());
                    //we set the service start as complete
                    mHasServiceStarted = 0;
                }
            }
        }

        //initialize fragments
        initializeFragments();

        //get the broadcast receivers for responses from the service
        IntentFilter intentFilter=new IntentFilter(LOCAL_BROADCAST_REC);
        intentFilter.addAction(ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceived, intentFilter);

        //toolbar
        Toolbar myToolbar = findViewById(R.id.tp_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //check if the service can be bound to
        if (mHasServiceStarted==1) {
            Intent serviceIntent = new Intent(this, ServiceFileShare.class);

            //we bind the service
            try {
                Log.d(TAG, "binding the service 1st");
                thisActivity.bindService(serviceIntent, serConnection, Context.BIND_AUTO_CREATE);
                mIsServiceBound = true;
            } catch (Exception e) {
                Log.e(TAG, "Couldnt bind service " + e.getMessage());
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG,"we save the value os has service started "+mHasServiceStarted);
        outState.putInt(SERVICE_STARTED_STATUS,mHasServiceStarted);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (mIsServiceBound) {
                Log.d(TAG, "Unbinding the service onStop");
                unbindService(serConnection);
                mIsServiceBound=false;
            }
        }catch (Exception e){
            Log.e(TAG,"Couldnt unbind the service "+e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"-------On destroy activity--------");

        //kill the fragment
        try {
            fragmentManager.beginTransaction().remove(fragmentFileTransferProgress);
        }catch (Exception e){
            Log.d(TAG,"couldnt remove the fragments on destroy");
        }

        //removing observers
        transferProgressActivityViewModel.getData().removeObservers(this);
        fileTransferViewModel.getFileListInfo().removeObservers(this);

        //dismiss dialogs if they exist
        try {
            mGeneralDialog.dismiss();
        }catch (Exception e){
            Log.d(TAG,"dialog cant be dismissed");
        }

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceived);
        }catch (Exception e){
            Log.d(TAG,"couldn't unregister the receiver "+e.getMessage());
        }

        super.onDestroy();
    }

    private void initializeFragments(){
        //manager
        fragmentManager=getSupportFragmentManager();

        //fragments
        fragmentFileTransferProgress=new FileTransferProgress();

        Bundle bundleFrag=new Bundle();
        bundleFrag.putInt(EXTRA_TYPE_TRANSFER,typeOfService);
        fragmentFileTransferProgress.setArguments(bundleFrag);

        //transaction
        fragmentManager.beginTransaction()
                .add(R.id.frag_atp_transfer_progress,fragmentFileTransferProgress)
                .commit();

        //we get the file model to populate the stuff
        fileTransferViewModel=ViewModelProviders.of(this).get(FileTransferViewModel.class);
        fileTransferViewModel.getFileListInfo().observe(this,fileTransferViewModelObserver);

        //we get the view model for the user transfer info
        transferProgressActivityViewModel=ViewModelProviders.of(this).get(TransferProgressActivityViewModel.class);
        transferProgressActivityViewModel.getData().observe(this,transferProgressActivityViewModelObserver);
    }

    //file observer
    final Observer<List<FileListEntry>> fileTransferViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we create a list for the not transferred and one for the transferred

            List<FileListEntry> tempSent=new ArrayList<>();
            List<FileListEntry> tempNotSent=new ArrayList<>();

            for (int i=0;i<fileListEntries.size();i++){
                if (fileListEntries.get(i).getIsTransferred()==0){
                    tempNotSent.add(fileListEntries.get(i));
                }else{
                    tempSent.add(fileListEntries.get(i));
                }
            }

            if (typeOfService==FILES_SENDING) {
                fragmentFileTransferProgress.updateRV(tempNotSent);
            }else{
                fragmentFileTransferProgress.updateRV(fileListEntries);
            }
        }
    };

    //user info observer
    final Observer<List<UserInfoEntry>> transferProgressActivityViewModelObserver=new Observer<List<UserInfoEntry>>() {
        @Override
        public void onChanged(List<UserInfoEntry> userInfoEntries) {
            //we check if the transfer has been completed before
            int isTransferInProgress=userInfoEntries.get(0).getIsTransferInProgress();
            Log.d(TAG,"It has changed, the value is "+isTransferInProgress);

            //if the app is relaunched and the transfer has finished and hasnt captured the broadcast events
            switch (isTransferInProgress){
                case STATUS_TRANSFER_FINISHED:
                    //transfer is over, show dialog and change button
                    //we show dialog that transfer is done
                    createDialog(R.string.service_finished_transfer);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();
                    //hide the progress bar
                    mProgressBarHide.setVisibility(View.GONE);
                    //set values to completed
                    fragmentFileTransferProgress.setComplete();

                    //fragmentManager.getFragments().get(0).

                    //we unbind the service
                    mHasServiceStarted=0;
                    doUnbind();

                    break;

                case STATUS_TRANSFER_OUT_OF_SPACE_ERROR:
                    mProgressBarHide.setVisibility(View.GONE);
                    //we show dialog we ran out of space and return to the main menu
                    createDialog(R.string.service_out_of_space_error);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();

                    //we unbind the service
                    mHasServiceStarted=0;
                    doUnbind();

                    break;

                case STATUS_TRANSFER_SOCKET_ERROR:
                    mProgressBarHide.setVisibility(View.GONE);
                    //we show dialog that there was an error and return to the main menu
                    createDialog(R.string.service_socket_error);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();

                    //we unbind the service
                    mHasServiceStarted=0;
                    doUnbind();

                    break;
                case STATUS_TRANSFER_NOTIFICATION_CANCEL:
                    reopenApp();
                    break;
            }
        }
    };

    //unbind service
    private void doUnbind(){
        if (mIsServiceBound) {
            try {
                unbindService(serConnection);
                mIsServiceBound = false;
            } catch (Exception e) {
                Log.e(TAG, "Couldnt unbind the service from observer" + e.getMessage());
            }
        }
    }

    //create OK dialog
    private void createDialog(int dialogText){
        //we show dialog that transfer is done
        if (mGeneralDialog!=null) {
            if (!mGeneralDialog.isShowing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                builder.setMessage(dialogText)
                        .setCancelable(true)
                        .setNeutralButton(R.string.gen_button_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });
                mGeneralDialog = builder.create();
            }
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
            builder.setMessage(dialogText)
                    .setCancelable(true)
                    .setNeutralButton(R.string.gen_button_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
            mGeneralDialog = builder.create();
        }

        try {
            mGeneralDialog.show();
        }catch (Exception e){
            Log.e(TAG,"Couldn't show dialog");
        }
    }

    //broadcast receiving
    private BroadcastReceiver mMessageReceived=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            //Log.d(TAG,"Received message from service "+action);

            //we check what to do depending on what the service needs to do
            if (action==ACTION_UPDATE_UI){
                mProgressBarHide.setVisibility(View.GONE);
                //update ui
                Bundle bundle=intent.getExtras();

                //send the data to the fragment
                fragmentFileTransferProgress.updateData(bundle);
            }
        }
    };

    //service connection
    private ServiceConnection serConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceFileShare.ServiceFileShareBinder binder = (ServiceFileShare.ServiceFileShareBinder) service;
            mService = binder.getService();

            //check if service is doing a transfer
            if (!mService.methodIsTransferActive()){
                Log.d(TAG,"the transfer is not active we hide the splash screen that hides everything");
                //it is not active
                mProgressBarHide.setVisibility(View.GONE);
            }else{
                //update the UI with data from the service
                mService.updateUIOnly();
            }

            Log.d(TAG,"Service has been bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"Service has been unbound");
            mIsServiceBound=false;
        }
    };

    @Override
    public void buttonOkCancel(String received){
        if (received.equals("Cancel")) {
            //create the dialog that will ask if yes or no
            AlertDialog.Builder cancelDialog = new AlertDialog.Builder(thisActivity);
            cancelDialog.setMessage("Are you sure you want to cancel?")
                    .setNegativeButton("NO", null)
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent serviceIntent=new Intent(thisActivity,ServiceFileShare.class);
                                    stopService(serviceIntent);
                                    reopenApp();
                                }
                            });

            mGeneralDialog=cancelDialog.create();

            if (!mGeneralDialog.isShowing()){
                mGeneralDialog.show();
            }
        }else{
            reopenApp();
        }
    }

    private void reopenApp(){
        //reset the file list
        FileListRepository fileListRepository=new FileListRepository(getApplication());
        fileListRepository.deleteTable();

        //change the status as inactive again
        transferProgressActivityViewModel.changeTransferStatus(STATUS_TRANSFER_INACTIVE);

        //reopen the activity
        Intent intent=new Intent(getApplicationContext(),MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}