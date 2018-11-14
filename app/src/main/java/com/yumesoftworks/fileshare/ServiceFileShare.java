package com.yumesoftworks.fileshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.yumesoftworks.fileshare.peerToPeer.ClientSocketTransfer;

import java.io.IOException;
import java.net.ServerSocket;

public class ServiceFileShare extends Service implements ClientSocketTransfer.ClientSocketTransferInterface {
    private static final String TAG="ServiceFileShare";

    //notification
    private static final String NOTIFICATION_CHANNEL="Main Channel";
    private static final int NOTIFICATION_ID=1002;
    private NotificationChannel channel;
    private NotificationManager manager;
    private int mTotalFiles;
    private int mCurrentFile;

    //socket stuff
    private ServerSocket mServerSocket;
    private ClientSocketTransfer mReceiverTransferSocket;
    private int mPort;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //check the API
        manager= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            //we set the channel
            channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }

        //we initialize the files
        mTotalFiles=0;
        mCurrentFile=0;

        //initialize the notification
        manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                ,getString(R.string.service_notification_text_initialize)
                ,false).build());

        //we get the bundle of extras
        Bundle receivedBundle=intent.getExtras();

        //we check if the intent is to send or to receive
        if (intent.getAction().equals(TransferProgressActivity.FILES_SENDING)){
            //we are sending files

            //we read the database

            //we start the socket for communication

        }else if (intent.getAction().equals(TransferProgressActivity.FILES_RECEIVING)){
            //we are receiving files
            //we start the server that will read the notification
            //we start the server socket
            try{
                //create the server socket
                mPort=receivedBundle.getInt(TransferProgressActivity.LOCAL_PORT);
                mServerSocket=new ServerSocket(mPort);

                //create the listener
                mReceiverTransferSocket=new ClientSocketTransfer(this,mServerSocket);
            }catch (IOException e){
                Log.d(TAG,"There was an error registering the server socket");
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //notification build
    private NotificationCompat.Builder notificationBuilder(String title, String filename, boolean showProgress){
        //we set the notification
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                    .setContentTitle(title)
                    .setContentText(filename)
                    .setSmallIcon(R.id.iv_avatar_icon)
                    .setProgress(mTotalFiles, mCurrentFile, showProgress)
                    .setAutoCancel(true);
    }

    //client interfaces
    @Override
    public void finishedProcessClient() {
        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent("finished");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void socketFailedClient() {
        //the socket failed
    }
}
