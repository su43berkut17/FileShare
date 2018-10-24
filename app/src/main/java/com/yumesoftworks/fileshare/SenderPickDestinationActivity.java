package com.yumesoftworks.fileshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yumesoftworks.fileshare.peerToPeer.BroadcastReceiverSender;

import java.util.List;

public class SenderPickDestinationActivity extends AppCompatActivity {

    private final IntentFilter intentFilter=new IntentFilter();
    private Channel mChannel;
    private WifiP2pManager mManager;
    private BroadcastReceiver receiver;

    private List peers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_pick_destination);

        //create the p2p connection
        createConnection();
    }

    private void createConnection(){
        //create the intent filters
        //Indicates a change in the Wi-Fi Peer-to-Peer status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        //ndicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        //Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        //Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        receiver=new BroadcastReceiverSender(mManager, mChannel,this);
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }
}
