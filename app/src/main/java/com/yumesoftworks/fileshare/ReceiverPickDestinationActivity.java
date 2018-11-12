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

    //database
    private UserInfoEntry mUserInfoEntry;

    //async socket
    private AsyncTask serverSocketTask;

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
        AppDatabase database=AppDatabase.getInstance(this);
        mUserInfoEntry=database.userInfoDao().loadUserInfo().getValue().get(0);

        //server socket
        try{
            mServerSocket=new ServerSocket(0);
        }catch (IOException e){
            Log.d(TAG,"There was an error registering the server socket");
        }

        mNsdHelper=new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerService(mServerSocket.getLocalPort());

        //execute the async task that waits for the client to ask for the information
        serverSocketTask=new AsyncTaskServer();
        serverSocketTask.execute();
    }

    //asynctask that runs on the server
    private class AsyncTaskServer extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {

            while (true) {
                // block the call until connection is created and return
                // Socket object
                try {
                    //wait for a connection
                    Socket socket = mServerSocket.accept();

                    Log.d(TAG,"Sending the user data");
                    ObjectOutputStream messageOut=new ObjectOutputStream(socket.getOutputStream());
                    messageOut.writeObject(mUserInfoEntry);

                }catch (Exception e){
                    Log.d(TAG,"the socket accept has failed");
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (mNsdHelper!=null){
            mNsdHelper.cancelPreviousRegRequest();
        }
        if (serverSocketTask!=null) {
            serverSocketTask.cancel(true);
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNsdHelper!=null){
            mNsdHelper.registerService(mServerSocket.getLocalPort());
        }
        if (serverSocketTask!=null){
            serverSocketTask.execute();
        }

    }
}
