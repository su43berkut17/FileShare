package com.yumesoftworks.fileshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.yumesoftworks.fileshare.peerToPeer.BroadcastReceiverSender;

import java.util.List;

public class SenderPickDestinationActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {

    private final static String TAG="SendPickActivity";

    private final IntentFilter intentFilter=new IntentFilter();
    private Channel mChannel;
    private WifiP2pManager mManager;
    private BroadcastReceiver receiver;

    private List peers;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

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

        //create the p2p connection
        createConnection();
    }

    private void createConnection(){
        //check is if it is compattible with p2p
        Log.d(TAG,"is it compatible with wifi "+getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT));

        //create the intent filters
        Log.d(TAG,"registering the intent filters");
        //Indicates a change in the Wi-Fi Peer-to-Peer status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        //indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        //Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        //Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        //discover peers
        /*mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"discover peers success");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG,"discover peer failure "+reason);
            }
        });*/
        //create a group
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        Log.d(TAG,"Group created");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG,"Error on group creation, code: "+reason);
            }
        });
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

    //on peers received
    public void getPeers(List peersRec){
        peers=peersRec;
    }

    //set the peer listener
    public WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
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
            }else{
                Log.d(TAG,"Devices found");
            }
        }
    };

    //set the connection info listener
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //we are connected, check what does it do
        Log.d(TAG,"the info is "+info.toString());
    }

    //update this device info
    public void updateThisDevice(WifiP2pDevice device){
        Log.d(TAG,"this device updated "+device.toString());
    }
}
