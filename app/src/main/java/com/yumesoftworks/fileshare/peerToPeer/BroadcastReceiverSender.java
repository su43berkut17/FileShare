package com.yumesoftworks.fileshare.peerToPeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;


import com.yumesoftworks.fileshare.SenderPickDestinationActivity;

import java.util.ArrayList;
import java.util.List;

public class BroadcastReceiverSender extends BroadcastReceiver {

    private static final String TAG="BroadcastReceiverSender";

    private WifiP2pManager manager;
    private Channel channel;
    private SenderPickDestinationActivity activity;
    private List peers = new ArrayList();

    public BroadcastReceiverSender(WifiP2pManager manager,
                                   WifiP2pManager.Channel channel,
                                   SenderPickDestinationActivity activity) {

        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi Direct mode is enabled or not
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //activity.setIsWifiP2pEnabled(true);
            } else {
                //activity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed
            Log.d(TAG,"On receive, WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION");
            if (manager != null) {
                Log.d(TAG,"On receive, manager is not null, we call the peer list update");
                manager.requestPeers(channel, activity.peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed
            //Broadcast intent action indicating that the state of Wi-Fi p2p connectivity has changed.
            // One extra EXTRA_WIFI_P2P_INFO provides the p2p connection info in the form of a WifiP2pInfo
            // object. Another extra EXTRA_NETWORK_INFO provides the network info in the form of
            // a NetworkInfo. A third extra provides the details of the group.
            //
            //See also:
            //
            //    EXTRA_WIFI_P2P_INFO
            //    EXTRA_NETWORK_INFO
            //    EXTRA_WIFI_P2P_GROUP
            //
            //Constant Value: "android.net.wifi.p2p.CONNECTION_STATE_CHANGE"
            if (manager==null){
                return;
            }

            //get a network info object for the correction
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()){
                //we are connected with the device so we request the info to find out who is the group owner
                manager.requestConnectionInfo(channel,activity);
            }else{
                //we disconnect
                Log.d(TAG,"Disconnect, what to do");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            //Broadcast intent action indicating that this device details have changed.

            /*DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));*/
            activity.getPeers(peers);
            activity.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }
    }




}
