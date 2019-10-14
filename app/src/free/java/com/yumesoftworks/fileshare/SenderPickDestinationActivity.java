package com.yumesoftworks.fileshare;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.SocketListEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.peerToPeer.SenderPickSocket;
import com.yumesoftworks.fileshare.recyclerAdapters.SendFileUserListAdapter;

import java.io.IOException;
import java.net.ServerSocket;
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
    private List<SocketListEntry> mSocketList;

    //for client socket
    private String localIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_pick_destination);

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
         MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_sender_pick_destination);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //storing local ip address
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wm.getConnectionInfo().getIpAddress();
        localIp = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));

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
        mSocketList=new ArrayList<>();

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

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.spd_toolbar);
        setSupportActionBar(myToolbar);

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

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isFirstExecution) {
            if (mNsdHelper != null) {
                mNsdHelper.initializeNsd();
                //mNsdHelper.registerService(mServerSocket.getLocalPort());
            }
        }
        isFirstExecution=false;
        mNsdHelper.discoverServices();
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
        //Log.d(TAG,"Comparing local IP: "+localIp+" with received: "+serviceInfo.getHost().getHostAddress());

        if (!localIp.equals(serviceInfo.getHost().getHostAddress())) {
            Log.d(TAG,"it is different we create");
            //we create the user
            UserSendEntry entry = new UserSendEntry("reading info...", 1, serviceInfo.getServiceName(), serviceInfo.getHost(), serviceInfo.getPort());

            //we push it to the temp
            //mTempUserList.add(entry)
            mUserList.add(entry);

            //we check the real information with the socket
            mSocketList.add(new SocketListEntry(serviceInfo.getServiceName(),serviceInfo.getHost().getHostAddress(),new SenderPickSocket(this,entry)));
        }
    }

    //update the data of the user
    @Override
    public void updateUserDataSocket(UserSendEntry userSendEntry) {
        //look in the list
        for (int i=0;i<mUserList.size();i++){
            if (mUserList.get(i).getInfoToSend().equals(userSendEntry.getInfoToSend())){
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
            if (mUserList.get(i).getInfoToSend().equals(serviceInfo.getServiceName())){
                Log.d(TAG,"We remove "+mUserList.get(i).getUsername());
                mUserList.remove(i);
            }
        }

        for (int j=0;j<mSocketList.size();j++){
            if (mSocketList.get(j).getServiceName().equals(serviceInfo.getServiceName())){
                mSocketList.get(j).getSenderSocket().destroySocket();
                mSocketList.remove(j);
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
    public void onItemClickListener(final int itemId) {
        //we send the message
         mSocketList.get(itemId).getSenderSocket().sendMessage(MESSAGE_OPEN_ACTIVITY);
    }

    @Override
    public void showErrorDialog() {
        Log.d(TAG, "Couldn't connect to the socket, we show dialog with error ");
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

    @Override
    public void openNextActivity(UserSendEntry sendEntry) {
        //close the socket
        try {
            mServerSocket.close();
        }catch (Exception e){
            Log.d(TAG,"cannot close main socket");
        }

        //we call the activity that will start the service with the info
        Intent intent = new Intent(this, TransferProgressActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        //data to send on the intent
        Bundle bundleSend = new Bundle();

        //local ip and port
        bundleSend.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_SENDING);
        bundleSend.putString(TransferProgressActivity.LOCAL_IP, mServerSocket.getInetAddress().toString());
        bundleSend.putInt(TransferProgressActivity.LOCAL_PORT, mServerSocket.getLocalPort());
        bundleSend.putString(TransferProgressActivity.REMOTE_IP, sendEntry.getIpAddress().getHostAddress());
        bundleSend.putInt(TransferProgressActivity.REMOTE_PORT, sendEntry.getPort());

        //we close and destroy all the sockets
        for (int i=0;i<mSocketList.size();i++){
            mSocketList.get(i).getSenderSocket().destroySocket();
        }

        try {
            mServerSocket.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        //open the activity
        Log.d(TAG, "Opening new activity with socket");
        intent.putExtras(bundleSend);
        startActivity(intent);
        finish();
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
