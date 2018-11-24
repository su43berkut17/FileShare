package com.yumesoftworks.fileshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserSendEntry;
import com.yumesoftworks.fileshare.peerToPeer.ReceiverSocketTransfer;
import com.yumesoftworks.fileshare.peerToPeer.SenderSocketTransfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

public class ServiceFileShare extends Service implements ReceiverSocketTransfer.ClientSocketTransferInterface {
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

    //loaded entry
    private List<FileListEntry> mFileListEntry;

    //counter
    private Boolean mInitDatabase=false;
    private Boolean mInitExtras=false;

    @Override
    public void onCreate() {

        super.onCreate();

        //we read the database
        AppDatabase database = AppDatabase.getInstance(this);
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
            mInitDatabase=true;
            beginTransfer();
            /*if (mInitDatabase==true && mInitExtras==true){
                beginTransfer();
            }*/
            return null;
        }
    }

    //we initiate the trasnfer
    private void beginTransfer(){
        Log.d(TAG,"database has been completed");
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

        //values
        String ipAddress = receivedBundle.getString(TransferProgressActivity.REMOTE_IP);
        int port =receivedBundle.getInt(TransferProgressActivity.REMOTE_PORT);

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
                        receivedBundle.getInt(TransferProgressActivity.REMOTE_PORT));

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

                    File file=new File(fileListEntries.get(mCurrentFile).getPath());
                    // Get the size of the file
                    long length = file.length();
                    byte[] bytes = new byte[16 * 1024];
                    InputStream in = new FileInputStream(file);
                    OutputStream out = socket.getOutputStream();

                    int count;
                    while ((count = in.read(bytes)) > 0) {
                        out.write(bytes, 0, count);
                    }

                    out.close();

                }*/

            }catch (Exception e){
                Log.d(TAG,"There was an error");
            }

        }else if (intent.getAction().equals(TransferProgressActivity.FILES_RECEIVING)){
            //we are receiving files
            try{
                //create the server socket
                mPort=receivedBundle.getInt(TransferProgressActivity.LOCAL_PORT);
                mServerSocket=new ServerSocket(mPort);

                //we create the socket listener
                mReceiverTransferSocket=new ReceiverSocketTransfer(this, mServerSocket);
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

    //client interfaces
    @Override
    public void finishedReceiveClient() {
        //the transfer is done, set dialog and go back to activity
        Intent intent=new Intent("finished");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void socketFailedClient() {
        //the socket failed

    }

    @Override
    public void updateSendUI(TextInfoSendObject textInfoSendObject) {
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
