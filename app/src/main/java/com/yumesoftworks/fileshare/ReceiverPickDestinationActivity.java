package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.peerToPeer.ReceiverPickSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ReceiverPickDestinationActivity extends AppCompatActivity implements ReceiverPickSocket.SocketReceiverConnectionInterface{

    private static final String TAG="ReceiverDesActivity";

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    //nds vars
    private NsdHelper mNsdHelper;
    private ServerSocket mServerSocket;

    //server socket
    private ReceiverPickSocket mReceiverSocket;

    //database
    private UserInfoEntry mUserInfoEntry;
    private AppDatabase mDb;
    private ReceiverPickDestinationViewModel viewModel;

    //lifecycle
    private Boolean isFirstExecution=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_pick_destination);

        //analytics
        // mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        /*MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_receiver_pick_destination);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        //we will use livedata for user
        mDb=AppDatabase.getInstance(getApplicationContext());
        setupViewModel();

        //we set the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    //view model
    private void setupViewModel(){
        viewModel=ViewModelProviders.of(this).get(ReceiverPickDestinationViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {
                mUserInfoEntry=userInfoEntries.get(0);
                initializeNsd();
            }
        });
    }

    @Override
    public void openNexActivity() {
        //we open the next activity with the socket information
        //we call the activity that will start the service with the info
        Intent intent=new Intent(this,TransferProgressActivity.class);

        //data to send on the intent
        Bundle bundleSend=new Bundle();

        //variables to be sent
        bundleSend.putString(TransferProgressActivity.EXTRA_TYPE_TRANSFER,TransferProgressActivity.FILES_SENDING);
        //bundleSend.putString(TransferProgressActivity.LOCAL_IP,mServerSocket.getInetAddress().getHostAddress());
        bundleSend.putInt(TransferProgressActivity.LOCAL_PORT,mServerSocket.getLocalPort());

        intent.putExtras(bundleSend);
        startActivity(intent);
    }

    //after loading database initialize discovery
    public void initializeNsd(){
        Log.d(TAG,"Initializing Nsd and sockets");
        //server socket
        try{
            mServerSocket=new ServerSocket(0);
        }catch (IOException e){
            Log.d(TAG,"There was an error registering the server socket");
        }

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerService(mServerSocket.getLocalPort());

        //we create the receiver pick socket
        if (mReceiverSocket==null) {
            mReceiverSocket = new ReceiverPickSocket(this,mServerSocket, mUserInfoEntry);
        }

        //we change the initial execution counter
        isFirstExecution=false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNsdHelper!=null){
            mNsdHelper.cancelPreviousRegRequest();
        }

        //we destroy the socket
        try {
            mReceiverSocket.destroySocket();
            mReceiverSocket=null;
        }catch (Exception e){
            Log.d(TAG,"Couldn't destroy socket on pause");
            mReceiverSocket=null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

        //we check if it is the initial execution
        if (!isFirstExecution) {
            if (mServerSocket != null) {
                //we resume the service discovery
                mNsdHelper.registerService(mServerSocket.getLocalPort());

                //we check if the receiver socket is null
                if (mReceiverSocket == null) {
                    Log.d(TAG, "recreating socket");
                    mReceiverSocket = new ReceiverPickSocket(this, mServerSocket, mUserInfoEntry);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //destroy the discovery
        mNsdHelper=null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
