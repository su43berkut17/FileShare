package com.yumesoftworks.fileshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.FileListRepository;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.yumesoftworks.fileshare.data.UserInfoRepository;
import com.yumesoftworks.fileshare.peerToPeer.TransferFileCoordinatorHelper;

public class ServiceFileShare extends Service implements
        TransferFileCoordinatorHelper.ReceiverSocketTransferInterfaceCoor,
        TransferFileCoordinatorHelper.SenderSocketTransferInterfaceCoor {
    private static final String TAG="ServiceFileShare";

    //notification
    private static final String NOTIFICATION_CHANNEL="Main Channel";
    private static final String NOTIFICATION_CHANNEL_FOREGROUND="Foreground Channel";
    private static final int NOTIFICATION_ID=1002;
    private static final int NOTIFICATION_FOREGROUND_ID=1003;
    private NotificationChannel channel;
    private NotificationChannel channelSilent;
    private NotificationManager manager;
    private int mTotalFiles=0;
    private int mCurrentFile=0;
    private String mCurrentFileName;
    private int mCounterTimesWidget=0;
    public static final String ACTION_STOP_SERVICE="stopServiceAction";
    public static final String ACTION_BEGIN_TRANSFER="startTransferAction";

    //socket stuff
    private TransferFileCoordinatorHelper mTransferFileCoordinatorHelper;
    private int mPort;

    //database access
    private FileListRepository repositoryFile;
    private UserInfoRepository repositoryUser;
    //private int mCurrentStatus;

    //threading
    private Thread readDataThread;

    //loaded entry
    private List<FileListEntry> mFileListEntry;

    //intent stuff
    private Bundle receivedBundle;
    private static Boolean isServiceStarted=false;
    private static Boolean isTransferActive=false;

    //service binding
    private final IBinder binder=new ServiceFileShareBinder();

    //type of service
    private int mAction=0;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"Creating service");
        //foreground service 1st
        //check the API
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //TODO: Check which notification solution makes notification appear in smart watch, might have to get rid of set ongoing so it appears on wearables
            //channel foreground silent
            channelSilent = new NotificationChannel(NOTIFICATION_CHANNEL_FOREGROUND,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channelSilent.setLightColor(Color.BLUE);
            channelSilent.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channelSilent);

            //we set the channel
            channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_FOREGROUND)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.service_notification_text_initialize))
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.icon_notification)
                    .build();

            try {
                startForeground(NOTIFICATION_FOREGROUND_ID, notification);
            }catch (Exception e){
                Log.e(TAG,"Couldn't start foreground notification");
                connectionError();
            }
        } else {
            manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                    , getString(R.string.app_name)
                    , false)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.icon_notification)
                    .build());
        }

        repositoryFile=new FileListRepository(getApplication());
        repositoryUser=new UserInfoRepository(getApplication());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind");

        //create the start foreground command
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_FOREGROUND)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSmallIcon(R.drawable.icon_notification)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build();

        startForeground(NOTIFICATION_FOREGROUND_ID, notification);

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    //bind
    public class ServiceFileShareBinder extends Binder {
        ServiceFileShare getService() {
            Log.d(TAG,"returning this service");
            // Return this instance of LocalService so clients can call public methods
            return ServiceFileShare.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"Destroying service");
        //we cancel everything

        //check if the current status is an error so it wont change it to success
        //if (mCurrentStatus != TransferProgressActivity.STATUS_TRANSFER_OUT_OF_SPACE_ERROR || mCurrentStatus != TransferProgressActivity.STATUS_TRANSFER_SOCKET_ERROR || mCurrentStatus!=TransferProgressActivity.STATUS_TRANSFER_NOTIFICATION_CANCEL) {
            //deactivate the switch transfer
            if (isServiceStarted && !isTransferActive) {
                repositoryUser.switchTransfer(TransferProgressActivity.STATUS_TRANSFER_FINISHED);
                repositoryUser.switchServiceType(TransferProgressActivity.SERVICE_TYPE_INACTIVE);
            }
        //}

        //make sure to destroy the transfer and threads by any chance if it is still active
        Boolean isItDestroyed;

        if (mTransferFileCoordinatorHelper!=null) {
            do {
                //try
                try {
                    isItDestroyed = mTransferFileCoordinatorHelper.userCancelled();
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't interrupt "+e.getMessage());
                        isItDestroyed=true;
                    }
                }catch (Exception e){
                    Log.e(TAG,"couldn't complete user cancelled action is destroyed");
                    isItDestroyed=true;
                }
            } while (isItDestroyed == false);
        }

        //return the widget to its normal state
        try {
            updateWidgetService.startActionUpdateWidget(this, TransferProgressWidget.STATE_NORMAL, "", 0, 0,100);
        }catch (Exception e){
            Log.e(TAG,"Couldn't set widget as normal");
        }

        isServiceStarted=false;
        isTransferActive=false;

        //cancel notification
        try {
            manager.cancel(NOTIFICATION_ID);
        }catch (Exception e){
            Log.e(TAG,"on Destroy Notification doesnt exist");
        }

        Log.d(TAG,"Service destroyed successfully");

        super.onDestroy();
    }

    //start the transfer
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //we set the service as started
        isServiceStarted=true;
        Log.d(TAG,"OnStart command");

        //get the intent
        try {
            receivedBundle = intent.getExtras();
        }catch (Exception e){
            receivedBundle=new Bundle();
        }

        //check if it is stopping the transfer
        if (intent.getAction()==ACTION_STOP_SERVICE){
            //we deactivate the transfer status
            Log.d(TAG,"trying to stop service by notification");
            switchTransfer(TransferProgressActivity.STATUS_TRANSFER_NOTIFICATION_CANCEL);

            //stop the transfer
            mTransferFileCoordinatorHelper.userCancelled();
        }else if (intent.getAction()==ACTION_BEGIN_TRANSFER){
            //load the database
            Log.d(TAG, "No active transfer, we load the database");
            //get the bundle
            receivedBundle=intent.getExtras();
            //change the active flag
            isTransferActive=true;
            loadDatabase();
        }else{
            //nothing happens, services keeps running
            Log.d(TAG, "Service running normally");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    //Load database
    private void loadDatabase(){
        //thread
        readDataThread=new Thread(new Runnable() {
            @Override
            public void run() {
                mFileListEntry=repositoryFile.getFilesDirect();
                Log.d(TAG,"Database loaded, file list entry is "+mFileListEntry);

                initializeService();
            }
        });

        readDataThread.start();
    }

    //change the switch and call the sockets
    private void initializeService(){
        //change the active on the file
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_ACTIVE);

        //we initialize the files
        mTotalFiles = 0;
        mCurrentFile = 0;

        initializeSockets();
    }

    //initialize sockets
    //should only work when database has been loaded and after receiving the intents
    private void initializeSockets(){
        Log.d(TAG,"initializing sockets");

        //get action
        try {
            mAction = receivedBundle.getInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER);
        }catch (Exception e){
            mAction=0;
        }
        Log.d(TAG,"Ready to initialize sockets");
        Log.d(TAG,"the action is "+mAction);

        //we check if the intent is to send or to receive
        if (mAction== com.yumesoftworks.fileshare.TransferProgressActivity.FILES_SENDING){
            //we are sending files
            //change to send or receive
            switchServiceType(TransferProgressActivity.SERVICE_TYPE_SENDING);
            //mFileListEntry=mFileListEntryLive;
            Log.d(TAG,"the value of the files is "+mFileListEntry.size());
            mTotalFiles=mFileListEntry.size();

            //we start the socket for communication
            try{
                mTransferFileCoordinatorHelper=new TransferFileCoordinatorHelper(this,
                        receivedBundle.getString(com.yumesoftworks.fileshare.TransferProgressActivity.REMOTE_IP),
                        receivedBundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.REMOTE_PORT),
                        mFileListEntry,mAction);

            }catch (Exception e){
                Log.e(TAG,"There was an error creating the send client socket");
                e.printStackTrace();
                connectionError();
            }
        }else if (mAction== com.yumesoftworks.fileshare.TransferProgressActivity.FILES_RECEIVING){
            //we are receiving files
            //change to send or receive
            switchServiceType(TransferProgressActivity.SERVICE_TYPE_RECEIVING);
            try{
                //create the server socket
                mPort=receivedBundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.LOCAL_PORT);

                mTransferFileCoordinatorHelper=new TransferFileCoordinatorHelper(this,mPort,mAction);
            }catch (Exception e){
                Log.e(TAG,"There was an error creating the receive client socket"+e.getMessage());
                e.printStackTrace();
                connectionError();
            }
        }else{
            Log.e(TAG,"We should never get to this");
        }
    }

    //notification build
    private NotificationCompat.Builder notificationBuilder(String title, String filename, boolean showProgress){
        //extras
        //set the extra
        Bundle extras=new Bundle();
        extras.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER,TransferProgressActivity.RELAUNCH_APP);

        //intent to open the activity
        Intent intentApp=new Intent(getApplicationContext(),TransferProgressActivity.class);
        intentApp.putExtras(extras);
        PendingIntent pendingIntentApp=PendingIntent.getActivity(this,0,intentApp,PendingIntent.FLAG_UPDATE_CURRENT);

        //intent to stop the transfer
        Intent intentStop=new Intent(this,ServiceFileShare.class);
        intentStop.setAction(ACTION_STOP_SERVICE);
        PendingIntent pendingIntentStopService=PendingIntent.getService(this,0,intentStop,PendingIntent.FLAG_UPDATE_CURRENT);

        //we set the notification
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                    .setContentTitle(title)
                    .setContentText(filename)
                    .setSmallIcon(R.drawable.icon_notification)
                    .setProgress(mTotalFiles, mCurrentFile, showProgress)
                    .setContentIntent(pendingIntentApp)
                    .setOngoing(true)
                    .addAction(R.drawable.icon_file_128,getString(R.string.service_notification_action_stop_service),pendingIntentStopService);

    }

    //dabatase stuff
    private void switchTransfer(int rActivateTransfer){
        Log.d(TAG,"Changing transfer to: "+rActivateTransfer);
        //mCurrentStatus=rActivateTransfer;
        repositoryUser.switchTransfer(rActivateTransfer);
    }
    private void switchServiceType(int rServiceType){
        repositoryUser.switchServiceType(rServiceType);
    }

    //successful sent
    private void addSuccessfulTransferCounter(){
        repositoryUser.addSuccessfulTransferCounter();
    }

    //socket error
    private void connectionError(){
        //the socket failed
        //we hide the notification
        try {
            manager.cancel(NOTIFICATION_ID);
        }catch (Exception e){
            Log.e(TAG,"Connection error couldnt cancel notification "+e.getMessage());
        }

        //we deactivate the transfer status
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_SOCKET_ERROR);

        isTransferActive=false;
    }

    //out of space
    private void transferErrorOutOfSpace(){
        //the socket failed
        //we hide the notification
        try {
            manager.cancel(NOTIFICATION_ID);
        }catch (Exception e){
            Log.e(TAG,"Out of space error couldnt cancel notification "+e.getMessage());
        }

        //we deactivate the transfer status
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_OUT_OF_SPACE_ERROR);

        isTransferActive=false;
    }

    //receive client interfaces
    @Override
    public void startedReceiveTransfer(){
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_ACTIVE);
    }

    public void finishedReceiveTransfer() {
        //we update the ui as successful
        Log.d(TAG,"Transfer receive finished");
        TextInfoSendObject endObject=new TextInfoSendObject(com.yumesoftworks.fileshare.TransferProgressActivity.TYPE_END,getResources().getString(R.string.service_success),String.valueOf(mTotalFiles)+","+String.valueOf(mTotalFiles));
        updateGeneralUI(endObject);

        //we update the notification
        manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                ,"Transfer successful"
                ,false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build());

        //we deactivate the transfer status
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_FINISHED);
        isTransferActive=false;

        //set the widget on its initial state
        try {
            updateWidgetService.startActionUpdateWidget(this, TransferProgressWidget.STATE_NORMAL, "", 0, 0,100);
        }catch (Exception e){
            Log.e(TAG,"Couldnt update widget back to normal "+e.getMessage());
        }
    }

    @Override
    public void socketReceiveFailedClient() {
        connectionError();
    }

    @Override
    public void errorReceiveNoSpace() {
        transferErrorOutOfSpace();
    }

    @Override
    public void updateReceiveSendUI(TextInfoSendObject textInfoSendObject) {
        updateGeneralUI(textInfoSendObject);
    }

    @Override
    public void updateReceivedFile(FileListEntry fileListEntry) {
        //new updateDatabaseReceivedAsyncTask(database).execute(fileListEntry);
        repositoryFile.saveFile(fileListEntry);
        Log.d(TAG,"Received file entry changed to transferred "+mFileListEntry);
    }

    @Override
    public void addReceivedCounter() {
        addSuccessfulTransferCounter();
    }

    //sender client interface
    @Override
    public void startedSenderTransfer(){
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_ACTIVE);
    }

    @Override
    public void updateSendSendUI(TextInfoSendObject textInfoSendObject) {
        updateGeneralUI(textInfoSendObject);
    }

    @Override
    public void finishedSendTransfer() {
        Log.d(TAG,"Transfer send finished");

        //we update the ui as successful
        TextInfoSendObject endObject=new TextInfoSendObject(com.yumesoftworks.fileshare.TransferProgressActivity.TYPE_END,getResources().getString(R.string.service_success),String.valueOf(mTotalFiles)+","+String.valueOf(mTotalFiles));
        updateGeneralUI(endObject);

        //we update the notification
        manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                ,"Transfer successful"
                ,false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build());

        //we set the database as not transferring so if they restart the app goes to the main menu
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_FINISHED);
        isTransferActive=false;

        //set the widget on its initial state
        try {
            updateWidgetService.startActionUpdateWidget(this, TransferProgressWidget.STATE_NORMAL, "", 0, 0,100);
        }catch (Exception e){
            Log.e(TAG,"Couldnt update widget back to normal "+e.getMessage());
        }
    }

    @Override
    public void socketSendFailedClient() {
        connectionError();
    }

    @Override
    public void errorSendNoSpace() {
        transferErrorOutOfSpace();
    }

    @Override
    public void addSentCounter() {
        addSuccessfulTransferCounter();
    }

    @Override
    public void updateSendSentFile(FileListEntry fileListEntry) {
        //set the file to is Transferred true
        repositoryFile.updateFileSetTransferred(fileListEntry);
        Log.d(TAG,"Sent file entry changed to transferred "+mFileListEntry);
    }

    //general methods
    private void updateGeneralUI(TextInfoSendObject textInfoSendObject){
        //we process the data received
        //name of file, current number and total number
        String fileName=textInfoSendObject.getMessageContent();
        String stringNumbers=textInfoSendObject.getAdditionalInfo();
        String[] currentNumbers = stringNumbers.split(",");
        String finalNotificationText=fileName+" "+currentNumbers[0]+" of "+currentNumbers[1];

        //we change the member variables of the progress
        int currentFile = Integer.parseInt(currentNumbers[0]);
        int totalFiles = Integer.parseInt(currentNumbers[1]);
        int percentage=0;
        if (totalFiles>0) {
            percentage = currentFile * 100 / totalFiles;
        }

        textInfoSendObject.setAdditionalInfo(currentFile+","+totalFiles+",0");

        //if this is the percentage of bytes
        if (currentNumbers.length > 3) {
            //percentage based on the bytes sent
            long totalBytes = Long.parseLong(currentNumbers[2]);
            long currentBytes = Long.parseLong(currentNumbers[3]);
            long percentageBytes = currentBytes * 100 / totalBytes;
            int percentageBytesInt = (int) percentageBytes;

            if (percentageBytesInt >= 100) {
                percentageBytesInt = 100;
            }

            //percentage based on the total
            int singlePercentage = 100 / totalFiles;

            //final percentage
            percentage = percentage + (percentageBytesInt * singlePercentage / 100);

            textInfoSendObject.setAdditionalInfo(currentFile+","+totalFiles+","+percentage);
        }else{
            //check if the total files is not 0
            if (totalFiles==0){
                textInfoSendObject.setAdditionalInfo(currentFile+","+totalFiles+","+0);
            }else{
                textInfoSendObject.setAdditionalInfo(currentFile+","+totalFiles+","+currentFile*100/totalFiles);
            }
        }

        //we change the member variables of the progress
        mTotalFiles=Integer.parseInt(currentNumbers[1]);
        mCurrentFile=Integer.parseInt(currentNumbers[0]);
        mCurrentFileName = fileName;

        //bundle
        Bundle bundle=new Bundle();
        bundle.putSerializable(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI_DATA,textInfoSendObject);

        //we update the notification
        manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                ,finalNotificationText
                ,true)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100,percentage,false)
                .build());

        //we update the UI
        Intent intent=new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        //update the widget
        //we will set a counter to prevent calling an update on the widget several times
        if (mCounterTimesWidget>30 || textInfoSendObject.getMessageType()==com.yumesoftworks.fileshare.TransferProgressActivity.TYPE_END) {
            Log.d(TAG,fileName+": "+currentNumbers.toString());
            mCounterTimesWidget=0;
            try {
                updateWidgetService.startActionUpdateWidget(this, TransferProgressWidget.STATE_TRANSFER, fileName, mTotalFiles, mCurrentFile,percentage);
            }catch (Exception e){
                Log.e(TAG,"Couldnt update widget "+e.getMessage());
            }
        }else{
            mCounterTimesWidget++;
        }
    }

    //activity asked for information
    public void updateUIOnly(){
        //only update if service is doing a transfer
        if (isTransferActive) {
            TextInfoSendObject textInfoSendObject = new TextInfoSendObject(0, mCurrentFileName,  mCurrentFile + "," + mTotalFiles);

            //bundle
            Bundle bundle = new Bundle();
            bundle.putSerializable(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI_DATA, textInfoSendObject);

            //we update the UI
            Intent intent = new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI);
            intent.putExtras(bundle);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    //activity asked is transfer is active
    public boolean methodIsTransferActive(){
        return isTransferActive;
    }

    //activity asked what kind of service it is
    public int typeOfService(){
        return mAction;
    }
}