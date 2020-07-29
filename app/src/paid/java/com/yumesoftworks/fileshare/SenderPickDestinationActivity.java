package com.yumesoftworks.fileshare;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yumesoftworks.fileshare.data.SocketListEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.peerToPeer.SenderPickSocket;
import com.yumesoftworks.fileshare.recyclerAdapters.SendFileUserListAdapter;

import java.io.IOException;
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
    private Context mContext;

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

    private TextView mConnectionStatus;

    //for client socket
    private String localIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_pick_destination);

        mContext=this;

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.spd_toolbar);
        setSupportActionBar(myToolbar);

        //we set the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mConnectionStatus=findViewById(R.id.spd_status);

        //Check wifi status
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm.isWifiEnabled()) {
            //storing local ip address
            int ipAddress = wm.getConnectionInfo().getIpAddress();
            localIp = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

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
                mConnectionStatus.setText(R.string.pu_message_initializing_connection);
            }catch (IOException e){
                Log.d(TAG,"There was an error registering the server socket");
                mConnectionStatus.setText(R.string.pu_message_connection_error);
            }

            mNsdHelper=new NsdHelper(this);
            mNsdHelper.discoverServices();

            isFirstExecution=true;
        }else{
            Toast.makeText(this,getText(R.string.pu_wifi_disabled),Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");

        //stop discovery
        if (mNsdHelper!=null){
            mNsdHelper.stopDiscovery();
        }

        //close all sockets
        try {
            mServerSocket.close();
        }catch (Exception e){
            Log.d(TAG,"Couldn't remove server socket");
        }

        //we empty the list
        try {
            mUserList.clear();
            mTempUserList.clear();
        }catch (Exception e){
            Log.e(TAG,"Values haven't been initialized.");
        }

        //close all the sockets on the list
        for (int i=0;i<mUserList.size();i++){
            mSocketList.get(i).getSenderSocket().destroySocket();
            mSocketList.get(i).getSenderSocket().removeCallbacks();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isFirstExecution) {
            if (mNsdHelper != null) {
                mNsdHelper.discoverServices();
            }
        }
        isFirstExecution=false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //callback
    //NSD Helper
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
    //From Sender Pick Socket
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

    //NSD Helper
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
                mSocketList.get(j).getSenderSocket().removeCallbacks();
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

    //NSD Helper
    @Override
    public void discoveryInitiated() {
        mConnectionStatus.setText(R.string.pu_instructions);
    }

    //NSD Helper
    @Override
    public void discoveryFailed() {
        mConnectionStatus.setText(R.string.pu_message_discovery_error);
    }

    //NSD Helper
    @Override
    public void serviceRegistered() {
        //No service registration required for sending
    }

    //NSD Helper
    @Override
    public void serviceRegistrationError() {
        //No service registration required for sending
    }

    //when the user has been clicked
    @Override
    public void onItemClickListener(final int itemId) {
        //we send the message
        mSocketList.get(itemId).getSenderSocket().sendMessage(MESSAGE_OPEN_ACTIVITY);
    }

    //From Sender Pick Socket
    @Override
    public void restartSocketConnection(Socket recSocket, UserSendEntry recEntry) {
        //look in the list
        for (int i=0;i<mUserList.size();i++){
            if (mSocketList.get(i).getIpAddress().equals(recEntry.getIpAddress().getHostAddress())){//chekc if it is the right ip
                //destroy and restart the socket
                mSocketList.get(i).getSenderSocket().destroySocket();
                mSocketList.get(i).getSenderSocket().removeCallbacks();
                mSocketList.get(i).setSenderSocket(new SenderPickSocket(this,recEntry));
            }
        }
    }

    //From Sender Pick Socket
    @Override
    public void showErrorDialog() {
        Log.d(TAG, "Couldn't connect to the socket, we show dialog with error ");
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,R.style.MyDialog);
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

    //From Sender Pick Socket
    @Override
    public void showConnectionError(){
        Toast.makeText(this,getText(R.string.pu_message_connection_error),Toast.LENGTH_SHORT).show();
        finish();
    }

    //From Sender Pick Socket
    @Override
    public void openNextActivity(UserSendEntry sendEntry) {
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