package com.yumesoftworks.fileshare;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.yumesoftworks.fileshare.data.AvatarDefaultImages;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.peerToPeer.NsdHelper;
import com.yumesoftworks.fileshare.peerToPeer.ReceiverPickSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ReceiverPickDestinationActivity extends AppCompatActivity implements ReceiverPickSocket.SocketReceiverConnectionInterface,
        NsdHelper.ChangedServicesListener{

    private static final String TAG="ReceiverDesActivity";

    //nds vars
    private NsdHelper mNsdHelper;
    private ServerSocket mServerSocket;

    //server socket
    private ReceiverPickSocket mReceiverSocket;

    //database
    private UserInfoEntry mUserInfoEntry;
    private ReceiverPickDestinationViewModel viewModel;

    //ui
    private TextView mUserName;
    private ImageView mUserIcon;
    private ImageView mConnectionAnimation;
    private TextView mConnectionStatus;

    private Context mContext;

    //prevent double launch app
    private boolean mLaunchNewActivity=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_pick_destination);

        mContext=this;

        //assign views
        mUserName=(TextView)findViewById(R.id.tv_receive_username);
        mUserIcon=(ImageView)findViewById(R.id.iv_receive_icon);
        mConnectionAnimation=(ImageView)findViewById(R.id.iv_receive_animation);
        mConnectionStatus=findViewById(R.id.tv_receive_wait);
        mConnectionStatus.setText(R.string.ru_message_initializing_connection);

        //Animation
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final AnimatedVectorDrawableCompat mLoadingAnimation=AnimatedVectorDrawableCompat.create(this,R.drawable.rpd_avd_waiting);
        mConnectionAnimation.setImageDrawable(mLoadingAnimation);
        mLoadingAnimation.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingAnimation.start();
                    }
                });
            }
        });
        mLoadingAnimation.start();

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.rpd_toolbar);
        setSupportActionBar(myToolbar);

        //we set the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void checkWifi(){
        //check wifi
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm.isWifiEnabled()) {
            //get file write access
            askForFilePermission();
        }else{
            Toast.makeText(this,getText(R.string.ru_wifi_disabled),Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initialize(){
        //we will use livedata for user
        setupViewModel();
    }

    private void askForFilePermission(){
        //we ask for permission before continuing
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //initialize values
                initialize();
            } else {
                //we ask for permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            //initialize values
            initialize();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //initialize values
            initialize();
        }else{
            //go back to main activity
            onBackPressed();
        }
    }

    //view model
    private void setupViewModel(){
        viewModel=ViewModelProviders.of(this).get(ReceiverPickDestinationViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {

                //we load the info
                mUserInfoEntry=userInfoEntries.get(0);

                //assign the info
                mUserName.setText(mUserInfoEntry.getUsername());

                //image
                List<AvatarStaticEntry> receivedAvatars = AvatarDefaultImages.getDefaultImages();
                String path=receivedAvatars.get(mUserInfoEntry.getPickedAvatar()).getPath();
                int imageUri = getApplicationContext().getResources().getIdentifier(path,"drawable",getApplicationContext().getPackageName());

                Glide.with(mContext).load(imageUri).into(mUserIcon);

                initializeNsd();
            }
        });
    }

    //after loading database initialize discovery
    public void initializeNsd(){
        Log.d(TAG,"Initializing Nsd and sockets");
        //server socket
        try{
            mServerSocket=new ServerSocket(0);
            mServerSocket.setSoTimeout(10000);
        }catch (IOException e){
            Log.d(TAG,"There was an error registering the server socket");
        }

        mNsdHelper = new NsdHelper(mContext);
        mNsdHelper.initializeRegistrationListener();
        mNsdHelper.registerService(mServerSocket.getLocalPort());

        //we create the receiver pick socket
        try{
            mReceiverSocket = new ReceiverPickSocket(this,mServerSocket, mUserInfoEntry);
        }catch (Exception e){
            Log.e(TAG,"Couldnt initialize the socket");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

        checkWifi();
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");
        super.onPause();

        //remove the observers
        if (viewModel!=null && viewModel.getUserInfo().hasObservers()){
            viewModel.getUserInfo().removeObservers(this);
        }

        if (mNsdHelper!=null){
            mNsdHelper.cancelRegistration();
        }

        //we destroy the socket
        try {
            if (mReceiverSocket.destroySocket()) {
                mReceiverSocket.removeCallbacks();
                mReceiverSocket = null;
                Log.d(TAG,"Removing callbacks an set to null");
            }
        }catch (Exception e){
            Log.d(TAG,"Couldn't destroy socket on pause");
            mReceiverSocket=null;
        }

        //destroy server socket
        try {
            mServerSocket.close();
            Log.d(TAG, "server socket is closed " + mServerSocket.isClosed());
        } catch (Exception e) {
            Log.d(TAG, "cant close server socket");
        }
    }

    @Override
    protected void onDestroy() {
        mNsdHelper=null;

        super.onDestroy();
    }

    @Override
    public void openNexActivity() {
        if (!mLaunchNewActivity) {
            mLaunchNewActivity=true;

            //we open the next activity with the socket information
            //we call the activity that will start the service with the info
            Intent intent = new Intent(this, TransferProgressActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            //data to send on the intent
            Bundle bundleSend = new Bundle();

            //variables to be sent
            bundleSend.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_RECEIVING);
            bundleSend.putInt(TransferProgressActivity.LOCAL_PORT, mServerSocket.getLocalPort());

            intent.putExtras(bundleSend);
            startActivity(intent);
            finish();
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

    @Override
    public void addedService(NsdServiceInfo serviceInfo) {
        //only for sender
    }

    @Override
    public void removedService(NsdServiceInfo serviceInfo) {
        //only for sender
    }

    @Override
    public void discoveryInitiated() {
        //only for sender
    }

    @Override
    public void discoveryFailed() {
        //only for sender
    }

    @Override
    public void serviceRegistered() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatus.setText(R.string.ru_tv_please_wait);
            }
        });
    }

    @Override
    public void serviceRegistrationError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatus.setText(R.string.ru_message_connection_error);
            }
        });
    }

    @Override
    public void restartReceiverPickSocket() {
        //we create the receiver pick socket
        try{
            mReceiverSocket.destroySocket();
            mReceiverSocket.removeCallbacks();
            mReceiverSocket = new ReceiverPickSocket(this,mServerSocket, mUserInfoEntry);
        }catch (Exception e){
            Log.e(TAG,"Couldnt initialize the socket");
        }
    }
}