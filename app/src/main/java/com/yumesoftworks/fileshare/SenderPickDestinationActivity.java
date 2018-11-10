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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class SenderPickDestinationActivity extends AppCompatActivity {

    private final static String TAG="SendPickActivity";

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    //nds vars
    private NsdHelper mNsdHelper;
    ServerSocket mServerSocket;

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

       //server socket
        try{
            mServerSocket=new ServerSocket(0);
        }catch (IOException e){

        }

        mNsdHelper=new NsdHelper(this, false);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerService(mServerSocket.getLocalPort());
        mNsdHelper.discoverServices();
    }

    @Override
    protected void onPause() {
        if (mNsdHelper!=null){
            mNsdHelper.stopDiscovery();
            mNsdHelper.cancelPreviousRegRequest();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNsdHelper!=null){
            mNsdHelper.registerService(mServerSocket.getLocalPort());
            mNsdHelper.discoverServices();
        }
    }
}
