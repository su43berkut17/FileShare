package com.yumesoftworks.fileshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.peerToPeer.SenderPickSocket;
import com.yumesoftworks.fileshare.recyclerAdapters.SendFileUserListAdapter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SenderPickDestinationActivity extends AppCompatActivity implements NsdHelper.ChangedServicesListener,
        SendFileUserListAdapter.ItemClickListener,
    SenderPickSocket.SocketSenderConnectionInterface{

    private final static String TAG="SendPickActivity";
    public final static String MESSAGE_OPEN_ACTIVITY="pleaseOpenANewActivity";

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    //nds vars
    private NsdHelper mNsdHelper;
    private ServerSocket mServerSocket;

    //we check if it is 1st execution
    private Boolean isFirstExecution=true;

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

    //for client socket
    private int mNumberOfItems;
    private int mCurrentSocketItem;
    //private AsyncTaskClient mSocketTask;

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

        isFirstExecution=true;

        //start process that checks every few seconds the updated list
        //mHandler=new Handler();
        //mDelayCheck=5*1000;

        //we set the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");
        if (mNsdHelper!=null){
            mNsdHelper.stopDiscovery();
            mNsdHelper.cancelRegistration();
            mNsdHelper.cancelResolver();
        }
        //we remove any callbacks
        try {
            mHandler.removeCallbacks(mRunnableCheck);
        }catch (Exception e){
            Log.d(TAG,"Cant remove callbacks "+e.getMessage());
        }
        //we cancel the task if it is paused, it will resume once the discovery begins
        /*if (mSocketTask!=null){
            mSocketTask.cancel(true);
        }*/

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isFirstExecution) {
            if (mNsdHelper != null) {
                mNsdHelper.initializeNsd();
                mNsdHelper.registerService(mServerSocket.getLocalPort());
            }
        }
        isFirstExecution=false;
        mNsdHelper.discoverServices();

        //we start the 1st discovery
        //startDiscoveryAndTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //we empty the list
        mUserList.clear();
        mTempUserList.clear();
    }

    //callback
    @Override
    public void addedService(NsdServiceInfo serviceInfo) {
        Log.d(TAG,"Received a service Info "+serviceInfo.getHost()+" ip "+serviceInfo.getHost().getHostAddress()+" local ip is "+mServerSocket.getInetAddress().getHostAddress());
        //we check the ip
        Log.d(TAG,"Comparing local IP: "+mServerSocket.getLocalSocketAddress().toString()+" with received: "+serviceInfo.getHost().getHostAddress());
        if (mServerSocket.getInetAddress().getHostAddress()!=serviceInfo.getHost().getHostAddress()) {
            //we create the user
            UserSendEntry entry = new UserSendEntry("reading info...", 1, serviceInfo.getServiceName(), serviceInfo.getHost(), serviceInfo.getPort());

            //we push it to the temp
            //mTempUserList.add(entry)
            mUserList.add(entry);

            //we check the real information with the socket
            SenderPickSocket senderPickSocket = new SenderPickSocket(this, entry);
        }
    }

    //update the data of the user
    @Override
    public void updateUserDataSocket(UserSendEntry userSendEntry) {
        //look in the list
        for (int i=0;i<mUserList.size();i++){
            if (mUserList.get(i).getInfoToSend()==userSendEntry.getInfoToSend()){
                mUserList.get(i).setUsername(userSendEntry.getUsername());
                mUserList.get(i).setAvatar(userSendEntry.getAvatar());
            }
        }

        //once it is done we update the adapter
        mAdapter.setUsers(mUserList);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void removedService(NsdServiceInfo serviceInfo) {
        Log.d(TAG,"Removing a service: "+serviceInfo.getServiceName());

        //cycle
        for (int i=0;i<mUserList.size();i++){
            Log.d(TAG,"comparing "+mUserList.get(i).getInfoToSend());
            if (mUserList.get(i).getInfoToSend()==serviceInfo.getServiceName()){
                Log.d(TAG,"We remove "+mUserList.get(i).getUsername());
                mUserList.remove(i);
            }
        }

        mAdapter.setUsers(mUserList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }


    //when the user has been clicked
    @Override
    public void onItemClickListener(int itemId) {
        //we call the activity that will start the service with the info
        Intent intent=new Intent(this,TransferProgressActivity.class);

        //get the current entry
        UserSendEntry sendEntry = mAdapter.getUserList().get(itemId);

        //data to send on the intent
        Bundle bundleSend=new Bundle();

        //local ip and port
        bundleSend.putString(TransferProgressActivity.EXTRA_TYPE_TRANSFER,TransferProgressActivity.FILES_SENDING);
        bundleSend.putString(TransferProgressActivity.LOCAL_IP,mServerSocket.getInetAddress().toString());
        bundleSend.putInt(TransferProgressActivity.LOCAL_PORT,mServerSocket.getLocalPort());
        bundleSend.putString(TransferProgressActivity.REMOTE_IP,sendEntry.getIpAddress().getHostAddress());
        bundleSend.putInt(TransferProgressActivity.REMOTE_PORT,sendEntry.getPort());

        //send
        String hostAddress=sendEntry.getIpAddress().getHostAddress();
        int hostIp=sendEntry.getPort();
        try {
            Log.d(TAG,"we create a socket "+hostAddress+" "+hostIp);
            Socket socket = new Socket(hostAddress, hostIp);
            Log.d(TAG,"socket is "+socket.toString());

            //send the info to go to the next stage to wait
            ObjectOutputStream messageOut=new ObjectOutputStream(socket.getOutputStream());
            messageOut.writeUTF(MESSAGE_OPEN_ACTIVITY);

            socket.close();

            //close the server socket
            try{
                mServerSocket.close();
            }catch (Exception e){
                Log.d(TAG,"Couldn't close the server socket");
            }

            //open the activity
            Log.d(TAG,"Opening new activity with socket");
            intent.putExtras(bundleSend);
            startActivity(intent);
        }catch (Exception e){
            Log.d(TAG,"Couldn't connect to the socket, we show dialog with error "+e.getMessage());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.pu_error_connect_dialog)
                    .setCancelable(true)
                    .setNeutralButton(R.string.gen_button_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.show();
        }
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
