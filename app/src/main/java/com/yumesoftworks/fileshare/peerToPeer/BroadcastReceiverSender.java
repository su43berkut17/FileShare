package com.yumesoftworks.fileshare.peerToPeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
            if (manager != null) {
                manager.requestPeers(channel, peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            /*DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));*/
            activity.getPeers(peers);
        }
    }

    //set the peer listener
    private PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            //debug only
            for (int i=0;i<peers.size();i++){
                Log.d(TAG,"number of peers: "+peers.get(i).toString());
            }

            // If an AdapterView is backed by this data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.
            //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
            if (peers.size() == 0) {
                Log.d(TAG, "No devices found");
                return;
            }
        }
    };
}
