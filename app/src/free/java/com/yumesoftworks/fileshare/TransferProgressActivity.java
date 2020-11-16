package com.yumesoftworks.fileshare;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
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

import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.FileListRepository;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.utils.UserConsent;

import java.util.ArrayList;
import java.util.List;

public class TransferProgressActivity extends AppCompatActivity implements
        FileTransferProgress.OnFragmentInteractionListener,
        UserConsent.UserConsentInterface {

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
    public static final String IS_ONLY_BIND="isOnlyBind";

    private LinearLayout mWaitingScreen;

    //this activity context
    private Context thisActivity;

    //constants for the actions
    public static final int FILES_SENDING=2001;
    public static final int FILES_RECEIVING=2002;
    public static final int RELAUNCH_APP=2003;

    //fragment parts
    private FileTransferProgress fragmentFileTransferProgress;
    private FragmentManager fragmentManager;

    //viewmodel
    private FileTransferViewModel fileTransferViewModel;
    private TransferProgressActivityViewModel transferProgressActivityViewModel;

    //Dialog
    private AlertDialog mGeneralDialog;

    //Ads
    private AdView mAdView;

    //type of service
    private int mTypeServiceOrRelaunch;
    private boolean mIsServiceBound=false;

    //from the intent that created the activity to indicate the service what kind of transfer
    private Bundle mExtras;

    //to check if we are ending the activity and service for good
    private boolean mAreWeClosing=false;

    //service binding
    private ServiceFileShare mService;

    //toolbar
    private FrameLayout frameRecycler;
    private AppBarLayout myToolbar;
    private ConstraintLayout header;
    private TextView mTvTitleCollapsed;
    private TextView mTvPercentageCollapsed;

    private TextView mTvTitle;
    private TextView mTvFileName;
    private TextView mTvOutOf;
    private TextView mtvPercentage;
    private ProgressBar mTvProgress;

    private int mContinuousPercentage;

    private float mDistX;
    private float mDistY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        thisActivity=this;
        Log.d(TAG,"onCreate called");

        //check the user consent
        UserConsent userConsent=new UserConsent(this);
        userConsent.checkConsent();

        //we get the instance of the indeterminate progress bar
        mWaitingScreen =findViewById(R.id.pb_atp_waitingForConnection);

        mAreWeClosing=false;//reset the closing flag

        //initialize the service
        Intent serviceIntent=new Intent(thisActivity,ServiceFileShare.class);
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
        }

        //get the extras
        try {
            Intent intent = getIntent();
            mExtras = intent.getExtras();
        }catch (Exception e){
            Log.e(TAG,"No extras found");
        }

        //get the broadcast receivers for responses from the service
        IntentFilter intentFilter=new IntentFilter(LOCAL_BROADCAST_REC);
        intentFilter.addAction(ACTION_UPDATE_UI);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceived, intentFilter);

        //toolbar
        myToolbar = findViewById(R.id.tp_app_bar_layout);
        header=findViewById(R.id.tp_header);
        mTvFileName=findViewById(R.id.tv_atp_filename);
        mTvTitle=findViewById(R.id.tv_atp_title);
        mTvTitleCollapsed=findViewById(R.id.tv_atp_title_collapsed);

        mtvPercentage=findViewById(R.id.tv_atp_percentage);
        mTvPercentageCollapsed =findViewById(R.id.tv_atp_percentage_collapsed);

        mTvOutOf=findViewById(R.id.tv_atp_files_out_of);
        mTvProgress=findViewById(R.id.pro_bar_atp);

        frameRecycler=findViewById(R.id.pb_atp_container_frame_layout);

        myToolbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener(){
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //Log.e(TAG,"Vertical offset: "+verticalOffset+appBarLayout.getTotalScrollRange()+" total "+appBarLayout.getTotalScrollRange());
                float value=verticalOffset+appBarLayout.getTotalScrollRange();
                value=value*100/appBarLayout.getTotalScrollRange();
                //Log.e(TAG,"percentage "+value);

                value=value/100;
                //Log.e(TAG,"divided by 100 "+value);

                if (mDistX==0) {
                    initializeToolbarPositions();
                }
                value=1-value;

                header.setAlpha(value);

                //shared translation for percentage
                float percentage=-((float)verticalOffset*100/(float)appBarLayout.getTotalScrollRange());

                mTvTitle.setTranslationY(-verticalOffset);
                mtvPercentage.setTranslationX(-mDistX*percentage/100);
                mtvPercentage.setTranslationY(-mDistY*percentage/100-verticalOffset);
                mtvPercentage.setAlpha(1-value);


                mTvPercentageCollapsed.setTranslationX(mDistX-mDistX*percentage/100);
                mTvPercentageCollapsed.setTranslationY(mDistY-mDistY*percentage/100);

                //fading
                mTvFileName.setAlpha(1-value);
                mTvOutOf.setAlpha(1-value);
                mTvProgress.setAlpha(1-value);
                mTvTitle.setAlpha(1-value);

                frameRecycler.setPadding(0,appBarLayout.getTotalScrollRange()+verticalOffset,0,0);
            }
        });

        //activate the marquee
        mTvFileName.setSelected(true);

        //initialize fragments
        initializeFragments();
    }

    //initialize positions of toolbar
    private void initializeToolbarPositions(){
        //initialize the positions
        int[] locationPercentage=new int[2];
        int[] locationPercentageCollapsed=new int[2];
        mtvPercentage.getLocationOnScreen(locationPercentage);
        mTvPercentageCollapsed.getLocationOnScreen(locationPercentageCollapsed);

        mDistX=locationPercentage[0]-locationPercentageCollapsed[0];
        mDistY=locationPercentage[1]-locationPercentageCollapsed[1];
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"-------On destroy activity--------");

        //removing observers
        removeObservers();

        //unbind service
        doUnbind();

        //kill the fragment
        try {
            fragmentManager.beginTransaction().remove(fragmentFileTransferProgress);
        }catch (Exception e){
            Log.d(TAG,"couldnt remove the fragments on destroy");
        }

        //dismiss dialogs if they exist
        try {
            mGeneralDialog.dismiss();
        }catch (Exception e){
            Log.d(TAG,"dialog cant be dismissed");
        }

        //remove broadcast manager
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
        try {
            int typeOfService = mExtras.getInt(EXTRA_TYPE_TRANSFER, 0);
            bundleFrag.putInt(EXTRA_TYPE_TRANSFER, typeOfService);
        }catch (Exception e){
            bundleFrag.putInt(EXTRA_TYPE_TRANSFER,0);
        }

        fragmentFileTransferProgress.setArguments(bundleFrag);

        //transaction
        fragmentManager.beginTransaction()
                .add(R.id.frag_atp_transfer_progress,fragmentFileTransferProgress)
                .commit();

        //we get the view model for the user transfer info
        transferProgressActivityViewModel= new ViewModelProvider(this).get(TransferProgressActivityViewModel.class);
        transferProgressActivityViewModel.getData().observe(this,transferProgressActivityViewModelObserver);
    }

    //file observer
    final Observer<List<FileListEntry>> fileTransferViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            fragmentFileTransferProgress.updateRV(fileListEntries);
        }
    };

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
                    setCompletedUI();

                    if (!mIsServiceBound) {
                        bindTheService();
                    }

                    break;

                case STATUS_TRANSFER_OUT_OF_SPACE_ERROR:
                    //we show dialog we ran out of space and return to the main menu
                    createDialog(R.string.service_out_of_space_error);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();

                    //hide the waiting screen
                    mWaitingScreen.setVisibility(View.GONE);

                    if (!mIsServiceBound) {
                        bindTheService();
                    }

                    break;

                case STATUS_TRANSFER_SOCKET_ERROR:
                    //we show dialog that there was an error and return to the main menu
                    createDialog(R.string.service_socket_error);

                    //change button to ok
                    fragmentFileTransferProgress.changeButton();

                    //hide the waiting screen
                    mWaitingScreen.setVisibility(View.GONE);

                    if (!mIsServiceBound) {
                        bindTheService();
                    }

                    break;

                case STATUS_TRANSFER_ACTIVE:
                    if (!mIsServiceBound) {
                        bindTheService();
                    }

                    break;

                case STATUS_TRANSFER_NOTIFICATION_CANCEL:
                    reopenApp();
                    break;

                case STATUS_TRANSFER_INACTIVE:
                    //check if the service is running
                    if (typeOfTransfer==TransferProgressActivity.SERVICE_TYPE_INACTIVE && !mAreWeClosing){
                        Log.d(TAG,"We start the service from observer");
                        startServiceTransfer();
                    }else if(mAreWeClosing){
                        Log.d(TAG,"We close the activity from observer and we reset everything");
                        //unbind
                        doUnbind();

                        //close the service
                        Intent serviceIntent=new Intent(thisActivity,ServiceFileShare.class);
                        stopService(serviceIntent);

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
                    break;
            }
        }
    };

    //Start service only when the status is inactive, that way we prevent the service from being
    //started if it is not needed
    private void startServiceTransfer(){
        Log.d(TAG,"Called start service");
        int typeOfService;
        try {
            typeOfService = mExtras.getInt(EXTRA_TYPE_TRANSFER, 0);
        }catch (Exception e){
            typeOfService=0;
        }
        Log.d(TAG,"The transfer type is: "+typeOfService+ " -- 2001 rec, 2002 for send, 0 for other");

        //service intent
        if (typeOfService==FILES_SENDING || typeOfService==FILES_RECEIVING) {
            Log.d(TAG,"Extras are good and service hasnt been started yet, we can start de service");
            Intent serviceIntent = new Intent(this, ServiceFileShare.class);
            serviceIntent.setAction(ServiceFileShare.ACTION_BEGIN_TRANSFER);
            serviceIntent.putExtras(mExtras);

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
        Bundle bindExtra=new Bundle();
        bindExtra.putBoolean(IS_ONLY_BIND,true);

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
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(thisActivity,R.style.MyDialog);
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
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(thisActivity,R.style.MyDialog);
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
            if (action.equals(ACTION_UPDATE_UI)){
                mWaitingScreen.setVisibility(View.GONE);
                //update ui
                Bundle bundle=intent.getExtras();
                TextInfoSendObject textInfoSendObject=(TextInfoSendObject) bundle.getSerializable(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI_DATA);

                try {
                    //name of file, current number and total number
                    String fileName = textInfoSendObject.getMessageContent();
                    String stringNumbers = textInfoSendObject.getAdditionalInfo();
                    String[] currentNumbers = stringNumbers.split(",");
                    String finalTextNumbers = currentNumbers[0] +" "+getString(R.string.atp_tv__number_connector)+" " + currentNumbers[1];

                    int percentage=0;
                    if (currentNumbers.length>2) {
                        percentage = Integer.parseInt(currentNumbers[2]);
                    }

                    if (mContinuousPercentage !=percentage && percentage<=100 && percentage>=1){
                        mContinuousPercentage = percentage;
                    }

                    //we update the data
                    if (!fileName.equals(mTvFileName.getText())) {
                        mTvFileName.setText(fileName);
                        mTvFileName.setSelected(true);
                    }
                    mTvOutOf.setText(finalTextNumbers);
                    mtvPercentage.setText(String.valueOf(mContinuousPercentage) + "%");
                    mTvPercentageCollapsed.setText(String.valueOf(mContinuousPercentage) + "%");
                    mTvProgress.setProgress(mContinuousPercentage);
                }catch (Exception e){
                    Log.e(TAG,"There was an exception while updating UI "+e.getMessage());
                    mTvFileName.setText("--");
                    mTvOutOf.setText("--");
                    mtvPercentage.setText("0%");
                    mTvPercentageCollapsed.setText("0%");
                    mTvProgress.setProgress(0);
                }
            }
        }
    };

    //update ui completed
    public void setCompletedUI(){
        Log.d(TAG,"Called on complete method to update to success");
        mTvFileName.setText(R.string.service_success);
        mTvOutOf.setText("");
        mtvPercentage.setText("100%");
        mTvProgress.setProgress(100);
    }

    //service connection
    private ServiceConnection serConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceFileShare.ServiceFileShareBinder binder = (ServiceFileShare.ServiceFileShareBinder) service;
            mService = binder.getService();

            //get the service type
            mTypeServiceOrRelaunch=mService.typeOfService();
            fragmentFileTransferProgress.transferType(mTypeServiceOrRelaunch);
            activateFileListObserver();

            //check if service is doing a transfer
            if (!mService.methodIsTransferActive()){
                Log.d(TAG,"the transfer is not active we hide the splash screen that hides everything");
                //it is not active
                mWaitingScreen.setVisibility(View.GONE);
            }else{
                //update the UI with data from the service
                mService.updateUIOnly();
            }

            mIsServiceBound = true;
            Log.d(TAG,"Service has been bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"Service has been unbound");
            mIsServiceBound=false;
        }
    };

    //before setting up the file list observer we need to know what kind of transfer it is
    // (sending or receiving) we can get that info from the service so we call this after we
    //bind the service
    void activateFileListObserver(){
        //we get the file model to get user data and transfer status
        fileTransferViewModel= new ViewModelProvider(this, new FileTransferViewModelFactory(this.getApplication())).get(FileTransferViewModel.class);
        fileTransferViewModel.getFileListInfo().observe(this,fileTransferViewModelObserver);
    }

    @Override
    public void buttonOkCancel(String received){
        if (received.equals("Cancel")) {
            //create the dialog that will ask if yes or no
            MaterialAlertDialogBuilder cancelDialog = new MaterialAlertDialogBuilder(thisActivity,R.style.MyDialog);
            cancelDialog.setMessage(R.string.ats_cancel_warning)
                    .setNegativeButton(R.string.gen_button_no, null)
                    .setPositiveButton(R.string.gen_button_yes,
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

    @Override
    public void initAd(Boolean isTracking) {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.ad_view_transfer_progress);
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