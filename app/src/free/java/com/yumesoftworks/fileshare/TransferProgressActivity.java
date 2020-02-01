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
    public static final int STATUS_TRANSFER_ACTIVE =3101;
    public static final int STATUS_TRANSFER_FINISHED =3102;
    public static final int STATUS_TRANSFER_SOCKET_ERROR =3201;
    public static final int STATUS_TRANSFER_OUT_OF_SPACE_ERROR =3202;
    public static final int STATUS_TRANSFER_NOTIFICATION_CANCEL =3301;

    //Service types
    public static final int SERVICE_TYPE_INACTIVE =4000;
    public static final int SERVICE_TYPE_SENDING =4001;
    public static final int SERVICE_TYPE_RECEIVING =4002;

    //broadcast actions
    public static final String ACTION_UPDATE_UI="updateUI";

    //name of bundle objects coming from service
    public static final String ACTION_UPDATE_UI_DATA="updateUIData";
    private static final String SERVICE_STARTED_STATUS="serviceStatusInitial";
    private static final String SEND_OR_RECEIVE_BUNDLE="sendOrReceiveBundle";

    private LinearLayout mWaitingScreen;

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

    //type of service
    private int mTypeServiceOrRelaunch;
    //private int mSendOrReceive;//useless
    private int mHasServiceStarted; //useless?
    private boolean mIsServiceBound=false;

    //from the intent that created the activity to indicate the service what kind of transfer
    private Bundle mExtras;

    //to check if we are ending the activity and service for good
    private boolean mAreWeClosing=false;

    //service binding
    private ServiceFileShare mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        thisActivity=this;
        Log.d(TAG,"onCreate called");

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_transfer_progress);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //we get the instance of the indeterminate progress bar
        mWaitingScreen =findViewById(R.id.pb_atp_waitingForConnection);

        mAreWeClosing=false;//reset the closing flag

        //get the extras
        try {
            Intent intent = getIntent();
            mExtras = intent.getExtras();
        }catch (Exception e){
            Log.e(TAG,"No extras found");
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
        removeObservers();

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

        //bundle to be sent to the new fragment
        Bundle bundleFrag=new Bundle();

        int typeOfService;

        try {
            typeOfService = mExtras.getInt(EXTRA_TYPE_TRANSFER);
            Log.d(TAG,"The transfer type is: "+typeOfService+ " -- 2001 rec, 2002 for send, 0 for others");
        }catch (Exception e){
            typeOfService=0;
            Log.e(TAG,"couldnt get what type of transfer it is so it is 0");
        }

        bundleFrag.putInt(EXTRA_TYPE_TRANSFER,typeOfService);

        fragmentFileTransferProgress.setArguments(bundleFrag);

        //transaction
        fragmentManager.beginTransaction()
                .add(R.id.frag_atp_transfer_progress,fragmentFileTransferProgress)
                .commit();

        //we get the file model to get user data and transfer status
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
            sortFilesBySendOrReceive(fileListEntries);
        }
    };

    //file observer sorting
    private void sortFilesBySendOrReceive(List<FileListEntry> fileListEntries){
        try {
            //we create a list for the not transferred and one for the transferred
            List<FileListEntry> tempSent = new ArrayList<>();
            List<FileListEntry> tempNotSent = new ArrayList<>();

            for (int i = 0; i < fileListEntries.size(); i++) {
                //files sent always will be added
                tempSent.add(fileListEntries.get(i));

                if (fileListEntries.get(i).getIsTransferred() == 0) {
                    tempNotSent.add(fileListEntries.get(i));
                }
            }
            Log.d(TAG, "Number of files to be sent: " + tempNotSent.size());
            Log.d(TAG, "Number of files sent: " + tempSent.size());
            Log.d(TAG, "Type of service: " + mTypeServiceOrRelaunch + " sending=2001 receiving =2002");
            if (mTypeServiceOrRelaunch == FILES_SENDING) {
                fragmentFileTransferProgress.updateRV(tempNotSent);
            } else if (mTypeServiceOrRelaunch == FILES_RECEIVING) {
                fragmentFileTransferProgress.updateRV(fileListEntries);
            }
        }catch (Exception e){
            Log.e(TAG,"failed to sort files by send or receive"+ e.getMessage());
        }
    }

    //user info observer
    final Observer<List<UserInfoEntry>> transferProgressActivityViewModelObserver=new Observer<List<UserInfoEntry>>() {
        @Override
        public void onChanged(List<UserInfoEntry> userInfoEntries) {
            //we check if the transfer has been completed before
            int isTransferInProgress=userInfoEntries.get(0).getIsTransferInProgress();
            int typeOfTransfer=userInfoEntries.get(0).getTransferTypeSendOrReceive();
            Log.d(TAG,"The is transfer in progress value is "+isTransferInProgress+" inactive 3000, active 3101, finished 3102, socket err 3201, space err 3202, cancel 3301");

            //if the app is relaunched and the transfer has finished and hasnt captured the broadcast events
            switch (isTransferInProgress){
                case STATUS_TRANSFER_FINISHED:
                    //transfer is over, show dialog and change button
                    //we show dialog that transfer is done
                    createDialog(R.string.service_finished_transfer);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();
                    //hide the waiting screen
                    mWaitingScreen.setVisibility(View.GONE);
                    //set values to completed
                    fragmentFileTransferProgress.setComplete();

                    //we unbind the service
                    mHasServiceStarted=0;
                    doUnbind();

                    break;

                case STATUS_TRANSFER_OUT_OF_SPACE_ERROR:
                    //we show dialog we ran out of space and return to the main menu
                    createDialog(R.string.service_out_of_space_error);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();

                    //hide the waiting screen
                    mWaitingScreen.setVisibility(View.GONE);

                    //we unbind the service
                    mHasServiceStarted=0;
                    doUnbind();

                    break;

                case STATUS_TRANSFER_SOCKET_ERROR:
                    //we show dialog that there was an error and return to the main menu
                    createDialog(R.string.service_socket_error);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();

                    //hide the waiting screen
                    mWaitingScreen.setVisibility(View.GONE);

                    //we unbind the service
                    mHasServiceStarted=0;
                    doUnbind();

                    break;

                case STATUS_TRANSFER_ACTIVE:
                    if (!mIsServiceBound && mHasServiceStarted==1) {
                        bindTheService();
                    }
                    break;

                case STATUS_TRANSFER_NOTIFICATION_CANCEL:
                    reopenApp();
                    break;

                case STATUS_TRANSFER_INACTIVE:
                    //check if the service is running
                    if (typeOfTransfer==TransferProgressActivity.SERVICE_TYPE_INACTIVE && mHasServiceStarted==0 && mAreWeClosing==false){
                        Log.d(TAG,"We start the service from observer");
                        startService();
                    }else if(mAreWeClosing==true){
                        //close for good
                        removeObservers();

                        //reset the file list
                        FileListRepository fileListRepository=new FileListRepository(getApplication());
                        fileListRepository.deleteTable();

                        //reopen the activity
                        Intent intent=new Intent(getApplicationContext(),MainMenuActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }

            }
        }
    };

    //Start service only when the status is inactive, that way we prevent the service from being
    //started if it is not needed
    private void startService(){
        Log.d(TAG,"Called start service");
        int typeOfService;
        try {
            typeOfService = mExtras.getInt(EXTRA_TYPE_TRANSFER);
            Log.d(TAG,"The transfer type is: "+typeOfService+ " -- 2001 rec, 2002 for send, 0 for other");
        }catch (Exception e){
            typeOfService=0;
            Log.e(TAG,"couldnt get what type of transfer it is");
        }

        //service intent
        if (mHasServiceStarted==0 && (typeOfService==FILES_SENDING || typeOfService==FILES_RECEIVING)) {
            Log.d(TAG,"Extras are good and service hasnt been started yet, we can start de service");
            Intent serviceIntent = new Intent(this, ServiceFileShare.class);
            serviceIntent.putExtras(mExtras);
            mHasServiceStarted = 1;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(TAG, "starting on create the service 1st");
                    startForegroundService(serviceIntent);
                } else {
                    Log.d(TAG, "starting on create the service 1st");
                    startService(serviceIntent);
                }

                //bind service
                bindTheService();

            } catch (Exception e) {
                Log.e(TAG, "Couldnt start service " + e.getMessage());
                //we set the service start as complete
                mHasServiceStarted = 0;
            }
        }else{
            //this is a relaunch when the service is shown as active but there are no bundles
            //probably after a crash, if that is the case, restart the app
            Log.d(TAG,"extras are bad, we dont start the service and we close the app");
            reopenApp();
        }
    }

    //binding service
    private void bindTheService(){
        //check if the service can be bound to
        Intent serviceIntent = new Intent(this, ServiceFileShare.class);

        //we bind the service
        try {
            Log.d(TAG, "binding the service 1st");
            thisActivity.bindService(serviceIntent, serConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Couldnt bind service " + e.getMessage());
        }
    }

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
                mWaitingScreen.setVisibility(View.GONE);
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
                mWaitingScreen.setVisibility(View.GONE);
            }else{
                //update the UI with data from the service
                mService.updateUIOnly();

                //check what is the type of service to communicate the fragment
                mTypeServiceOrRelaunch=mService.typeOfService();
                fragmentFileTransferProgress.transferType(mTypeServiceOrRelaunch);
                //update the viewmodel
                try {
                    sortFilesBySendOrReceive(fileTransferViewModel.getFileListInfo().getValue());
                }catch (Exception e){
                    Log.e(TAG,"cant separate send from receive yet");
                }
                mIsServiceBound = true;
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
        Log.d(TAG,"closing the app via reopenApp");

        //change the closing flag
        mAreWeClosing=true;

        //change the status as inactive again
        //transferProgressActivityViewModel.changeTransferStatus(STATUS_TRANSFER_INACTIVE);
        //transferProgressActivityViewModel.changeServiceTypeStatus(SERVICE_TYPE_INACTIVE);
        transferProgressActivityViewModel.setAsInactive();
    }

    private void removeObservers(){
        Log.d(TAG,"removing the observers");
        //removing observers
        if (transferProgressActivityViewModel!=null && transferProgressActivityViewModel.getData().hasObservers()){
            transferProgressActivityViewModel.getData().removeObservers(this);
        }
        if (fileTransferViewModel!=null && fileTransferViewModel.getFileListInfo().hasObservers()) {
            fileTransferViewModel.getFileListInfo().removeObservers(this);
        }
    }
}