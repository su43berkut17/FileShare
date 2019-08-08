package com.yumesoftworks.fileshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.FileListRepository;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

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
    private int mTotalFiles;
    private int mCurrentFile;

    //socket stuff
    private TransferFileCoordinatorHelper mTransferFileCoordinatorHelper;
    private int mPort;

    //database access
    private AppDatabase database;
    private FileListRepository repositoryFile;
    private UserInfoRepository repositoryUser;

    //loaded entry
    private List<FileListEntry> mFileListEntry;

    //intent stuff
    private Bundle receivedBundle;

    @Override
    public void onCreate() {

        super.onCreate();

        repositoryFile=new FileListRepository(getApplication());
        repositoryUser=new UserInfoRepository(getApplication());

        //we read the database
        mFileListEntry=repositoryFile.getFiles().getValue();
        Log.d(TAG,"Database loaded, file list entry is "+mFileListEntry);

        initializeSockets();

        //database = AppDatabase.getInstance(this);
        //new saveDatabaseAsyncTask(database).execute();
    }

    /*private class saveDatabaseAsyncTask extends AsyncTask<Void,Void,Void> {
        private AppDatabase database;

        saveDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mFileListEntry=database.fileListDao().loadFileListDirect();
            Log.d(TAG,"Database loaded, file list entry is "+mFileListEntry);

            initializeSockets();
            return null;
        }
    }*/

    //start the transfer
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
            Notification notification=new NotificationCompat.Builder(getApplicationContext(),NOTIFICATION_CHANNEL)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.service_notification_text_initialize)).build();
            startForeground(NOTIFICATION_ID,notification);
        }else{
            manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                    ,getString(R.string.service_notification_text_initialize)
                    ,false).build());
        }

        //we initialize the files
        mTotalFiles=0;
        mCurrentFile=0;

        //we get the bundle of extras
        receivedBundle=intent.getExtras();

        //we call the initialize sockets
        initializeSockets();

        return super.onStartCommand(intent, flags, startId);
    }

    //initialize sockets
    //should only work when database has been loaded and after receiving the intents
    private void initializeSockets(){
        if (mFileListEntry!=null && receivedBundle!=null){
            //do stuff
            //get action
            int action=receivedBundle.getInt(TransferProgressActivity.ACTION_SERVICE);
            Log.d(TAG,"the action is "+action);

            //we check if the intent is to send or to receive
            if (action==TransferProgressActivity.FILES_SENDING){
                //we are sending files
                //mFileListEntry=mFileListEntryLive;
                Log.d(TAG,"the value of the files is "+mFileListEntry.size());
                mTotalFiles=mFileListEntry.size();

                //we start the socket for communication
                try{
                    mTransferFileCoordinatorHelper=new TransferFileCoordinatorHelper(this,
                            receivedBundle.getString(TransferProgressActivity.REMOTE_IP),
                            receivedBundle.getInt(TransferProgressActivity.REMOTE_PORT),
                            mFileListEntry,action);

                }catch (Exception e){
                    Log.d(TAG,"There was an error creating the send client socket");
                    e.printStackTrace();
                    connectionError();
                }

            }else if (action==TransferProgressActivity.FILES_RECEIVING){
                //we are receiving files
                try{
                    //create the server socket
                    mPort=receivedBundle.getInt(TransferProgressActivity.LOCAL_PORT);

                    mTransferFileCoordinatorHelper=new TransferFileCoordinatorHelper(this,mPort,action);
                }catch (Exception e){
                    Log.d(TAG,"There was an error creating the receive client socket"+e.getMessage());
                    e.printStackTrace();
                    connectionError();
                }
            }
        }
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
                    .setSmallIcon(R.drawable.logo_128)
                    .setProgress(mTotalFiles, mCurrentFile, showProgress)
                    .setAutoCancel(true);
    }

    //dabatase stuff
    private void switchTransfer(Boolean activateTransfer){

        repositoryUser.switchTransfer(activateTransfer);
        //we switch the transfer status to on or off
        //new updateDatabaseAsyncTask(database).execute(activateTransfer);
    }

    /*private class updateDatabaseAsyncTask extends AsyncTask<Boolean,Void,Void> {
        private AppDatabase database;

        updateDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Boolean... params) {
            int status;
            //we read the status
            if (params[0]){
                status=1;
            }else{
                status=0;
            }

            UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
            userInfoEntry.setIsTransferInProgress(status);
            database.userInfoDao().updateTask(userInfoEntry);
            Log.d(TAG,"The transfer file status activation is: "+status);

            return null;
        }
    }*/

    //successful sent
    private void addSuccessfulTransferCounter(){
        repositoryUser.addSuccessfulTransferCounter();
        //new updateDatabaseCounterAsyncTask(database).execute();
    }

    /*
    //save transfer
    private class updateDatabaseCounterAsyncTask extends AsyncTask<Void,Void,Void> {
        private AppDatabase database;

        updateDatabaseCounterAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            UserInfoEntry userInfoEntry=database.userInfoDao().loadUserWidget().get(0);
            int currentCount=userInfoEntry.getNumberFilesTransferred();
            currentCount++;
            userInfoEntry.setNumberFilesTransferred(currentCount);
            database.userInfoDao().updateTask(userInfoEntry);
            Log.d(TAG,"We add a number more to the total transfers: "+currentCount);

            return null;
        }
    }*/

    //socket error
    private void connectionError(){
        //the socket failed
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(false);

        //set error dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_SOCKET_ERROR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    //receive client interfaces
    @Override
    public void startedReceiveTransfer(){
        switchTransfer(true);
    }

    public void finishedReceiveTransfer() {
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(false);

        //set the widget on its initial state
        updateWidgetService.startActionUpdateWidget(this,TransferProgressWidget.STATE_NORMAL,"",0,0);

        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_FINISHED_TRANSFER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    @Override
    public void socketReceiveFailedClient() {
        connectionError();
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

    /*private class updateDatabaseReceivedAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        updateDatabaseReceivedAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final FileListEntry... params) {
            //mFileListEntry=database.fileListDao().loadFileListDirect()

            database.fileListDao().insertFile(params[0]);

            Log.d(TAG,"Received file entry changed to transferred "+mFileListEntry);
            return null;
        }
    }*/

    @Override
    public void addReceivedCounter() {
        addSuccessfulTransferCounter();
    }

    //sender client interface
    @Override
    public void startedSenderTransfer(){
        switchTransfer(true);
    }

    @Override
    public void updateSendSendUI(TextInfoSendObject textInfoSendObject) {
        updateGeneralUI(textInfoSendObject);
    }

    @Override
    public void finishedSendTransfer() {
        //we update the ui as successful
        TextInfoSendObject endObject=new TextInfoSendObject(TransferProgressActivity.TYPE_END,getResources().getString(R.string.service_success),String.valueOf(mTotalFiles)+","+String.valueOf(mTotalFiles));
        updateGeneralUI(endObject);

        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we set the database as not transferring so if they restart the app i goes to the main menu
        switchTransfer(false);

        //set the widget on its initial state
        updateWidgetService.startActionUpdateWidget(this,TransferProgressWidget.STATE_NORMAL,"",0,0);

        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_FINISHED_TRANSFER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    @Override
    public void socketSendFailedClient() {
        connectionError();
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

    /*private class updateDatabaseSentAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        updateDatabaseSentAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final FileListEntry... params) {
            //mFileListEntry=database.fileListDao().loadFileListDirect()
            FileListEntry updateEntry=params[0];
            updateEntry.setIsTransferred(1);
            database.fileListDao().updateFile(updateEntry);

            Log.d(TAG,"Sent file entry changed to transferred "+mFileListEntry);
            return null;
        }
    }*/

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

        //bundle
        Bundle bundle=new Bundle();
        bundle.putSerializable(TransferProgressActivity.ACTION_UPDATE_UI_DATA,textInfoSendObject);

        //we update the notification
        manager.notify(NOTIFICATION_ID, notificationBuilder(getString(R.string.app_name)
                ,finalNotificationText
                ,true).build());

        //we update the UI
        Intent intent=new Intent(TransferProgressActivity.ACTION_UPDATE_UI);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        //update the widget
        updateWidgetService.startActionUpdateWidget(this,TransferProgressWidget.STATE_TRANSFER,fileName,mTotalFiles,mCurrentFile);
    }
}
