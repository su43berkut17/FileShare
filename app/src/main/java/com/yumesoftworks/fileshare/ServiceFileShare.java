package com.yumesoftworks.fileshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
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
    private static final int NOTIFICATION_ID=1002;
    private NotificationChannel channel;
    private NotificationManager manager;
    private int mTotalFiles=0;
    private int mCurrentFile=0;
    private String mCurrentFileName;
    private int mCounterTimesWidget=0;

    //socket stuff
    private TransferFileCoordinatorHelper mTransferFileCoordinatorHelper;
    private int mPort;

    //database access
    //private AppDatabase database;
    private FileListRepository repositoryFile;
    private UserInfoRepository repositoryUser;

    //loaded entry
    private List<FileListEntry> mFileListEntry;

    //intent stuff
    private Bundle receivedBundle;
    private Boolean isServiceStarted=false;

    //service binding
    private final IBinder binder=new ServiceFileShareBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        repositoryFile=new FileListRepository(getApplication());
        repositoryUser=new UserInfoRepository(getApplication());

        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_ACTIVE);

        new loadDatabaseAsyncTask().execute();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"Destroying service");
        //we cancel everything
        //cancel notification
        try {
            manager.cancel(NOTIFICATION_ID);
        }catch (Exception e){
            Log.d(TAG,"Notification doesnt exist");
        }

        //deactivate the switch transfer
        if (isServiceStarted) {
            repositoryUser.switchTransfer(TransferProgressActivity.STATUS_TRANSFER_FINISHED);
        }

        Boolean isItDestroyed;

        if (mTransferFileCoordinatorHelper!=null) {
            do {
                isItDestroyed = mTransferFileCoordinatorHelper.userCancelled();
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't interrupt");
                }
            } while (isItDestroyed == false);
        }

        isServiceStarted=false;

        super.onDestroy();
    }

    private class loadDatabaseAsyncTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(final Void... params) {
            mFileListEntry=repositoryFile.getFilesDirect();
            Log.d(TAG,"Database loaded, file list entry is "+mFileListEntry);

            initializeSockets();
            return null;
        }
    }

    //start the transfer
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //we check is the service has been started
        if (!isServiceStarted) {
            //change the flag
            isServiceStarted=true;

            //check the API
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //we set the channel
                channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                        getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setLightColor(Color.BLUE);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
                Notification notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.service_notification_text_initialize))
                        .setOnlyAlertOnce(true)
                        .build();
                startForeground(NOTIFICATION_ID, notification);
            } else {
                manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                        , getString(R.string.service_notification_text_initialize)
                        , false)
                        .setOnlyAlertOnce(true)
                        .build());
            }

            //we initialize the files
            mTotalFiles = 0;
            mCurrentFile = 0;

            //we get the bundle of extras
            try {
                //we call the initialize sockets
                receivedBundle = intent.getExtras();
                initializeSockets();
            } catch (Exception e) {
                Log.d(TAG, "No extra information sent to the service, we stop it.");
                stopSelf();
            }
        }else{
            Log.d(TAG,"Service already started");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //bind
    public class ServiceFileShareBinder extends Binder {
        ServiceFileShare getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServiceFileShare.this;
        }
    }

    //initialize sockets
    //should only work when database has been loaded and after receiving the intents
    private void initializeSockets(){
        if (mFileListEntry!=null && receivedBundle!=null){
            //do stuff
            //get action
            int action=receivedBundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_SERVICE);
            Log.d(TAG,"the action is "+action);

            //we check if the intent is to send or to receive
            if (action== com.yumesoftworks.fileshare.TransferProgressActivity.FILES_SENDING){
                //we are sending files
                //mFileListEntry=mFileListEntryLive;
                Log.d(TAG,"the value of the files is "+mFileListEntry.size());
                mTotalFiles=mFileListEntry.size();

                //we start the socket for communication
                try{
                    mTransferFileCoordinatorHelper=new TransferFileCoordinatorHelper(this,
                            receivedBundle.getString(com.yumesoftworks.fileshare.TransferProgressActivity.REMOTE_IP),
                            receivedBundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.REMOTE_PORT),
                            mFileListEntry,action);

                }catch (Exception e){
                    Log.d(TAG,"There was an error creating the send client socket");
                    e.printStackTrace();
                    connectionError();
                }

            }else if (action== com.yumesoftworks.fileshare.TransferProgressActivity.FILES_RECEIVING){
                //we are receiving files
                try{
                    //create the server socket
                    mPort=receivedBundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.LOCAL_PORT);

                    mTransferFileCoordinatorHelper=new TransferFileCoordinatorHelper(this,mPort,action);
                }catch (Exception e){
                    Log.d(TAG,"There was an error creating the receive client socket"+e.getMessage());
                    e.printStackTrace();
                    connectionError();
                }
            }
        }
    }

    //notification build
    private NotificationCompat.Builder notificationBuilder(String title, String filename, boolean showProgress){
        //we set the notification
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                    .setContentTitle(title)
                    .setContentText(filename)
                    .setSmallIcon(R.drawable.logo_128)
                    .setProgress(mTotalFiles, mCurrentFile, showProgress)
                    .setAutoCancel(true);
    }

    //dabatase stuff
    private void switchTransfer(int activateTransfer){
        repositoryUser.switchTransfer(activateTransfer);
    }

    //successful sent
    private void addSuccessfulTransferCounter(){
        repositoryUser.addSuccessfulTransferCounter();
    }

    //socket error
    private void connectionError(){
        //the socket failed
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_SOCKET_ERROR);

        //set error dialog and go back to activity
        Intent intent=new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_SOCKET_ERROR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    //out of space
    private void transferErrorOutOfSpace(){
        //the socket failed
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_OUT_OF_SPACE_ERROR);

        //set error dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_OUT_OF_SPACE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    //receive client interfaces
    @Override
    public void startedReceiveTransfer(){
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_ACTIVE);
    }

    public void finishedReceiveTransfer() {
        //we update the ui as successful
        TextInfoSendObject endObject=new TextInfoSendObject(com.yumesoftworks.fileshare.TransferProgressActivity.TYPE_END,getResources().getString(R.string.service_success),String.valueOf(mTotalFiles)+","+String.valueOf(mTotalFiles));
        updateGeneralUI(endObject);

        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_FINISHED);

        //set the widget on its initial state
        updateWidgetService.startActionUpdateWidget(this,TransferProgressWidget.STATE_NORMAL,"",0,0);

        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_FINISHED_TRANSFER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
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
        //we update the ui as successful
        TextInfoSendObject endObject=new TextInfoSendObject(com.yumesoftworks.fileshare.TransferProgressActivity.TYPE_END,getResources().getString(R.string.service_success),String.valueOf(mTotalFiles)+","+String.valueOf(mTotalFiles));
        updateGeneralUI(endObject);

        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we set the database as not transferring so if they restart the app i goes to the main menu
        switchTransfer(TransferProgressActivity.STATUS_TRANSFER_FINISHED);

        //set the widget on its initial state
        updateWidgetService.startActionUpdateWidget(this,TransferProgressWidget.STATE_NORMAL,"",0,0);

        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_FINISHED_TRANSFER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
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
        //new updateDatabaseSentAsyncTask(database).execute(fileListEntry);
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
        mTotalFiles=Integer.parseInt(currentNumbers[1]);
        mCurrentFile=Integer.parseInt(currentNumbers[0]);
        mCurrentFileName =fileName;

        //bundle
        Bundle bundle=new Bundle();
        bundle.putSerializable(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI_DATA,textInfoSendObject);

        //we update the notification
        manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                ,finalNotificationText
                ,true)
                .setOnlyAlertOnce(true)
                .build());

        //we update the UI
        Intent intent=new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        //update the widget
        //we will set a counter to prevent calling an update on the widget several times
        if (mCounterTimesWidget>20 || textInfoSendObject.getMessageType()==com.yumesoftworks.fileshare.TransferProgressActivity.TYPE_END) {
            Log.d(TAG,fileName+": "+currentNumbers.toString());
            mCounterTimesWidget=0;
            updateWidgetService.startActionUpdateWidget(this, TransferProgressWidget.STATE_TRANSFER, fileName, mTotalFiles, mCurrentFile);
        }else{
            mCounterTimesWidget++;
        }
    }

    //activity asked for information
    public void updateUIOnly(){
        TextInfoSendObject textInfoSendObject=new TextInfoSendObject(0, mCurrentFileName, mCurrentFileName +","+mTotalFiles);

        //bundle
        Bundle bundle=new Bundle();
        bundle.putSerializable(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI_DATA,textInfoSendObject);

        //we update the UI
        Intent intent=new Intent(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
