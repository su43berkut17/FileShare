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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.peerToPeer.ReceiverSocketTransfer;
import com.yumesoftworks.fileshare.peerToPeer.SenderSocketTransfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ServiceFileShare extends Service implements
        ReceiverSocketTransfer.ClientSocketTransferInterface,
        SenderSocketTransfer.SenderSocketTransferInterface {
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
    private ReceiverSocketTransfer mReceiverTransferSocket;
    private SenderSocketTransfer mSenderTransferSocket;
    private int mPort;

    //database access
    private AppDatabase database;

    //loaded entry
    private List<FileListEntry> mFileListEntry;

    @Override
    public void onCreate() {

        super.onCreate();

        //we read the database
        database = AppDatabase.getInstance(this);
        new saveDatabaseAsyncTask(database).execute();
    }

    private class saveDatabaseAsyncTask extends AsyncTask<Void,Void,Void> {
        private AppDatabase database;

        saveDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mFileListEntry=database.fileListDao().loadFileListDirect();
            Log.d(TAG,"Database loaded, file list entry is "+mFileListEntry);
            return null;
        }
    }

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
                mSenderTransferSocket = new SenderSocketTransfer(getApplicationContext()
                        ,receivedBundle.getString(TransferProgressActivity.REMOTE_IP),
                        receivedBundle.getInt(TransferProgressActivity.REMOTE_PORT),
                        mFileListEntry);

                /*Socket socket=new Socket(ipAddress,port);

                //now we send the 1st data which is an object with the number of files to be transferred
                //send the info to go to the next stage to wait
                ObjectOutputStream messageOut=new ObjectOutputStream(socket.getOutputStream());
                messageOut.writeInt(fileListEntries.size());

                //cycle to send each file
                Boolean isOver=false;

                while(!isOver){
                    //we check what is the client telling us


                    //we send the file



                }*/

            }catch (Exception e){
                Log.d(TAG,"There was an error creating the send client socket");
            }

        }else if (intent.getAction().equals(TransferProgressActivity.FILES_RECEIVING)){
            //we are receiving files
            try{
                //create the server socket
                mPort=receivedBundle.getInt(TransferProgressActivity.LOCAL_PORT);
                mServerSocket=new ServerSocket(mPort);

                //we create the socket listener
                try {
                    mReceiverTransferSocket = new ReceiverSocketTransfer(this, mServerSocket);
                }catch (Exception e){
                    Log.d(TAG,"There was an error creating the receive server socket");
                }
            }catch (IOException e){
                Log.d(TAG,"There was an error registering the server socket "+e.getMessage());
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
                    .setSmallIcon(R.drawable.logo_128)
                    .setProgress(mTotalFiles, mCurrentFile, showProgress)
                    .setAutoCancel(true);
    }

    //dabatase stuff
    private void switchTransfer(Boolean activateTransfer){
        //we switch the transfer status to on or off
        new updateDatabaseAsyncTask(database).execute(activateTransfer);
    }

    private class updateDatabaseAsyncTask extends AsyncTask<Boolean,Void,Void> {
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

            return null;
        }
    }

    //receive client interfaces
    @Override
    public void startedReceiveTransfer(){
        switchTransfer(true);
    }

    public void finishedReceiveClient() {
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(false);
        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_FINISHED_TRANSFER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void socketReceiveFailedClient() {
        //the socket failed
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(false);

        //set error dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_SOCKET_ERROR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void updateReceiveSendUI(TextInfoSendObject textInfoSendObject) {
        updateGeneralUI(textInfoSendObject);
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
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we set the database as not transferring so if they restart the app i goes to the main menu
        switchTransfer(false);

        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_FINISHED_TRANSFER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void socketErrorSend() {
        //the socket failed
        //we hide the notification
        manager.cancel(NOTIFICATION_ID);

        //we deactivate the transfer status
        switchTransfer(false);

        //set error dialog and go back to activity
        Intent intent=new Intent(TransferProgressActivity.ACTION_SOCKET_ERROR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void updateSendSentFile(FileListEntry fileListEntry) {
        //set the file to is Transferred true
        new updateDatabaseSentAsyncTask(database).execute(fileListEntry);
    }

    private class updateDatabaseSentAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        updateDatabaseSentAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final FileListEntry... params) {
            //mFileListEntry=database.fileListDao().loadFileListDirect()
            FileListEntry updateEntry=params[0];
            database.fileListDao().updateFile(updateEntry);

            Log.d(TAG,"Sent file entry changed to transferred "+mFileListEntry);
            return null;
        }
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
    }
}
