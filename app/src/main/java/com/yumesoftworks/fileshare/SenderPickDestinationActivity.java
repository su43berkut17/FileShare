package com.yumesoftworks.fileshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.UserSendEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.recyclerAdapters.SendFileUserListAdapter;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class SenderPickDestinationActivity extends AppCompatActivity implements NsdHelper.ChangedServicesListener,
        SendFileUserListAdapter.ItemClickListener {

    private final static String TAG="SendPickActivity";

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    //nds vars
    private NsdHelper mNsdHelper;
    private ServerSocket mServerSocket;

    //recyclerview
    private RecyclerView mRecyclerView;
    private SendFileUserListAdapter mAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private List<UserSendEntry> mUserList;
    private List<UserSendEntry> mTempUserList;

    //handler for discovery every n seconds
    private Handler mHandler;
    private Runnable mRunnableCheck;
    private int mDelayCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_pick_destination);

        //analytics
        //mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        /* MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_sender_pick_destination);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
        Log.d(TAG,"initializing nsd");

        //create recycler view and adapter
        mRecyclerView=findViewById(R.id.rv_sdpa_destinations);
        mLinearLayoutManager=new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mAdapter=new SendFileUserListAdapter(this,this);
        mRecyclerView.setAdapter(mAdapter);

        //create a new list
        mUserList=new ArrayList<>();
        mTempUserList=new ArrayList<>();

       //server socket
        try{
            mServerSocket=new ServerSocket(0);
        }catch (IOException e){
            Log.d(TAG,"There was an error registering the server socket");
        }

        mNsdHelper=new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerService(mServerSocket.getLocalPort());
        //mNsdHelper.discoverServices();
        //start process that checks every few seconds the updated list
        mHandler=new Handler();
        mDelayCheck=5*1000;
    }

    @Override
    protected void onPause() {
        if (mNsdHelper!=null){
            mNsdHelper.stopDiscovery();
            mNsdHelper.cancelPreviousRegRequest();
        }
        //we remove any callbacks
        mHandler.removeCallbacks(mRunnableCheck);

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNsdHelper!=null){
            mNsdHelper.registerService(mServerSocket.getLocalPort());
        }

        //we start the 1st discovery
        startDiscoveryAndTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //we empty the list
        mUserList.clear();
        mTempUserList.clear();
    }

    //timer
    private void startDiscoveryAndTimer(){
        Log.d(TAG,"Running the discovery and Timer");
        //clear the user list
        mUserList.clear();
        //we move the temp list to the adapter user list
        Boolean exists;
        for (int i=0;i<mTempUserList.size();i++){
            //push the 1st item
            if (i==0) {
                mUserList.add(mTempUserList.get(i));
            }else{
                //loop that checks with every item if it has been added already
                exists=false;
                Log.d(TAG,"Will start comparing "+mTempUserList.get(i).getIpAddress().getHostAddress());

                for (int j=0;j<mUserList.size();j++){
                    Log.d(TAG,"comparing "+mTempUserList.get(i).getIpAddress().getHostAddress()+" with:"+mUserList.get(j).getIpAddress().getHostAddress());
                    if (mTempUserList.get(i).getIpAddress().getHostAddress()==mUserList.get(j).getIpAddress().getHostAddress()){
                        exists=true;
                    }
                }

                //we check if it doesnt exist
                if (exists==false){
                    mUserList.add(mTempUserList.get(i));
                }
            }
        }

        //we assign the adapter to the normal list
        mAdapter.setUsers(mUserList);
        mAdapter.notifyDataSetChanged();

        //we start method that will load the right data in the recycler views

        //we clear the temp user list
        mTempUserList.clear();

        //we start the discovery again
        mNsdHelper.discoverServices();

        //we start the handler
        mHandler.postDelayed(mRunnableCheck = new Runnable() {
            @Override
            public void run() {
                //clear the temp list
                startDiscoveryAndTimer();
            }
        },mDelayCheck);
    }

    //callback
    @Override
    public void addedService(NsdServiceInfo serviceInfo) {
        Log.d(TAG,"Received a service Info "+serviceInfo.getHost());
        //we create the user
        UserSendEntry entry=new UserSendEntry("reading info...",1,"",serviceInfo.getHost(), serviceInfo.getPort());

        //we push it to the temp
        mTempUserList.add(entry);
    }

    //when the user has been clicked
    @Override
    public void onItemClickListener(int itemId) {
        //we call the activity that will start the service with the info

    }
}
