package com.yumesoftworks.fileshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class ServiceFileShare extends Service {
    private static final String TAG="ServiceFileShare";

    //notification
    private static final String NOTIFICATION_CHANNEL="Main Channel";
    private static final int NOTIFICATION_ID=101010;
    private NotificationChannel channel;
    private NotificationManager manager;
    private int mTotalFiles;
    private int mCurrentFile;

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

        //we check if the intent is to send or to receive
        if (intent.getAction().equals(TransferProgressActivity.FILES_SENDING)){
            //we are sending files

        }else if (intent.getAction().equals(TransferProgressActivity.FILES_RECEIVING)){
            //we are receiving files

        }
        return super.onStartCommand(intent, flags, startId);
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
