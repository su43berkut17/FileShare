package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;

public class TransferProgressActivity extends AppCompatActivity implements FileTransferProgress.OnFragmentInteractionListener,
FileTransferSent.OnFragmentInteractionListener{

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
    public static final int TYPE_FILE_BYTES=1003;
    public static final int TYPE_FILE_TRANSFER_SUCCESS=1004;

    //broadcast actions
    public static final String ACTION_FINISHED_TRANSFER="finishedTransfer";
    public static final String ACTION_UPDATE_UI="updateUI";
    public static final String ACTION_SOCKET_ERROR="connectionError";

    //name of bundle objects coming from service
    public static final String ACTION_UPDATE_UI_DATA="updateUIData";

    private LinearLayout mProgressBarHide;

    //when sending


    //when receiving, the list


    //constants for the actions
    public static final int FILES_SENDING=2001;
    public static final int FILES_RECEIVING=2002;
    public static final int RELAUNCH_APP=2003;
    public static final String ACTION_SERVICE ="ReceivingFiles";

    //fragment parts
    private FileTransferProgress fragmentFileTransferProgress;
    private FileTransferSent fragmentFileTransferSent;
    private FragmentManager fragmentManager;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;

    //viewmodel
    private FileTransferViewModel fileTransferViewModel;
    private AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        //analytics
        /*mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_transfer_progress);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        //we get the instance of the indeterminate progress bar
        mProgressBarHide=findViewById(R.id.pb_atp_waitingForConnection);

        //we check the intent with the information to start the service
        Intent intent=getIntent();

        //intent
        Intent serviceIntent=new Intent(this,ServiceFileShare.class);
        Bundle extras=intent.getExtras();

        //get the data to see how do we start the service
        int typeOfService=extras.getInt(EXTRA_TYPE_TRANSFER);


        if (typeOfService==RELAUNCH_APP){
            //nothing happens since everything has been initialized

        }else {
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
            serviceIntent.putExtras(extras);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            //initialize fragments
            initializeFragments();

            //get the broadcast receivers for responses from the service
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceived, new IntentFilter(LOCAL_BROADCAST_REC));
        }
    }

    private void initializeFragments(){
        //manager
        fragmentManager=getSupportFragmentManager();

        //fragments
        fragmentFileTransferProgress=new FileTransferProgress();
        fragmentFileTransferSent=new FileTransferSent();

        //transaction
        fragmentManager.beginTransaction()
                .add(R.id.frag_atp_transfer_progress,fragmentFileTransferProgress)
                .add(R.id.frag_atp_transfer_sent,fragmentFileTransferSent)
                .commit();

        //we get the file model to populate the stuff
        fileTransferViewModel=ViewModelProviders.of(this).get(FileTransferViewModel.class);
        fileTransferViewModel.getFileListInfo().observe(this,fileTransferViewModelObserver);
    }

    //observer
    final Observer<List<FileListEntry>> fileTransferViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we create a list for the not transferred and one for the transferred
            fragmentFileTransferProgress.updateRV(fileListEntries);
            fragmentFileTransferSent.updateRV(fileListEntries);
        }
    };

    //broadcast receiving
    private BroadcastReceiver mMessageReceived=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();

            //we check what to do depending on what the service needs to do
            switch (action){
                case ACTION_UPDATE_UI:
                    //update ui
                    Bundle bundle=intent.getExtras();

                    //send the data to the fragment
                    fragmentFileTransferProgress.updateData(bundle);

                    break;
                case ACTION_FINISHED_TRANSFER:
                    //we show dialog that transfer is done
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.service_finished_transfer)
                            .setCancelable(true)
                            .setNeutralButton(R.string.gen_button_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //we go to the main activity
                                            Intent intent=new Intent(getApplicationContext(),WelcomeScreenActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    });
                    builder.show();
                    break;
                case ACTION_SOCKET_ERROR:
                    //we show dialog that there was an error and return to the main menu
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                    builder2.setMessage(R.string.service_socket_error)
                            .setCancelable(true)
                            .setNeutralButton(R.string.gen_button_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //we go to the main activity
                                            Intent intent=new Intent(getApplicationContext(),WelcomeScreenActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    });
                    builder2.show();
                    break;
            }
        }
    };


    @Override
    public void onFragmentInteractionSent(Uri uri) {

    }

    @Override
    public void onFragmentInteractionProgress(Uri uri){

    }
}
