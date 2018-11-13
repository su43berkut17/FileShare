package com.yumesoftworks.fileshare;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.peerToPeer.ReceiverPickSocket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverPickDestinationActivity extends AppCompatActivity {

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
    private AppDatabase database;
    private DatabaseAsyncTask mDatabaseTask;

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

        //load the database data to be sent (name and number of avatar)
        database=AppDatabase.getInstance(this);

        //we start the task on the background
        mDatabaseTask=new DatabaseAsyncTask();
        //mDatabaseTask.execute();
    }

    //class that loads the database
    private class DatabaseAsyncTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG,"Loading the database");
            mUserInfoEntry=database.userInfoDao().loadUserWidget().get(0);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //we initialize the sockets
            Log.d(TAG,"On post execute of the async of the database, we will initialize the sockets");
            initializeNsd();
            super.onPostExecute(aVoid);
        }
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
            mReceiverSocket = new ReceiverPickSocket(mServerSocket, mUserInfoEntry);
        }
    }

    @Override
    protected void onPause() {
        if (mNsdHelper!=null){
            mNsdHelper.cancelPreviousRegRequest();
        }

        //we destroy the socket
        //mReceiverSocket.destroySocket();
        //mReceiverSocket=null;

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //we destroy the database asynctask
        //mDatabaseTask.cancel(true);
        //mDatabaseTask=null;

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (mNsdHelper!=null){
            //mNsdHelper.cancelPreviousRegRequest();
            //mNsdHelper.registerService(mServerSocket.getLocalPort());
        }

        //we excute the database read again
        mDatabaseTask.execute();

        super.onResume();
    }
}
