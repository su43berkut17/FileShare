package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;

import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.common.util.ArrayUtils;
import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

public class ReceiverSocketTransfer {
    private static final String TAG="ServiceClientSocket";

    //actions
    private static final int ACTION_SEND_MESSAGE=1001;
    private static final int ACTION_RECEIVE_FILE=1002;
    private static final int ACTION_RECEIVE_DETAILS=1003;
    private static final int ACTION_CONFIRM_DETAILS=1004;
    private static final int ACTION_NEXT_ACTION=1005;

    //local server socket
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private int mPort;

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //current action
    private int mCurrentAction;

    //current file
    private TextInfoSendObject mTextInfoSendObject;
    private String mCurrentFileSize;
    private String mCurrentFile;
    private String mTotalFiles;

    //interface
    private ReceiverSocketTransferInterface mReceiverInterface;

    public ReceiverSocketTransfer(TransferFileCoordinatorHelper context, int port){
        mPort=port;
        //mReceiverInterface=(ReceiverSocketTransferInterface) context;
        mReceiverInterface=(ReceiverSocketTransferInterface) context;

        socketHandler=new Handler(Looper.getMainLooper());
        socketThread=new Thread(new CommunicationThread());
        socketThread.start();
    }

    class CommunicationThread implements Runnable{
        @Override
        public void run() {
            Boolean doWeRepeat=true;//to retry if needed
            int totalSocketRetries=20;
            int currentSocketRetries=0;

            while(doWeRepeat){
                // Socket object
                try {
                    //wait for a connection
                    mServerSocket=new ServerSocket(mPort);
                    mServerSocket.setReuseAddress(true);

                    Log.d(TAG, "Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mSocket = mServerSocket.accept();

                    Log.d(TAG,"Socket has connected successfully");

                    //we reset the retries
                    currentSocketRetries = 0;

                    //we initialize the 1st action
                    mCurrentAction=ACTION_RECEIVE_DETAILS;

                    //we initialize the stream objects
                    ObjectOutputStream messageOut=new ObjectOutputStream(mSocket.getOutputStream());
                    ObjectInputStream messageIn=new ObjectInputStream(mSocket.getInputStream());
                    InputStream fileInputStream=mSocket.getInputStream();
                    FileOutputStream fileOutputStream;

                    //loop for sending and receiving
                    do{
                        if (mCurrentAction==ACTION_SEND_MESSAGE || mCurrentAction==ACTION_CONFIRM_DETAILS) {
                            //we send message that the transfer has been successful
                            try {
                                if (mCurrentAction==ACTION_SEND_MESSAGE) {
                                    TextInfoSendObject textInfoSendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_TRANSFER_SUCCESS, "", "");
                                    //messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                    messageOut.writeObject(textInfoSendObject);

                                    //reset action to next file
                                    mCurrentAction=ACTION_NEXT_ACTION;

                                    Log.d(TAG,"File transfer successful");
                                }else if(mCurrentAction==ACTION_CONFIRM_DETAILS){
                                    Log.d(TAG,"confirming sent details");
                                    TextInfoSendObject textInfoSendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS_SUCCESS, "", "");
                                    messageOut.writeObject(textInfoSendObject);

                                    //reset action to receive file
                                    mCurrentAction=ACTION_RECEIVE_FILE;
                                }

                            } catch (Exception e) {
                                Log.d(TAG, "There is no output stream " + e.getMessage());
                            }
                        }

                        //receiving
                        if (mCurrentAction==ACTION_RECEIVE_DETAILS){
                            //we read the object
                            try {
                                Log.d(TAG,"receiving details");
                                //messageIn = new ObjectInputStream(mSocket.getInputStream());
                                TextInfoSendObject message = (TextInfoSendObject) messageIn.readObject();
                                mTextInfoSendObject=message;

                                //extract the size of the file and set again
                                String stringNumbers=mTextInfoSendObject.getAdditionalInfo();
                                String[] currentNumbers = stringNumbers.split(",");

                                mCurrentFile=currentNumbers[0];
                                mTotalFiles=currentNumbers[1];
                                mCurrentFileSize=currentNumbers[2];

                                //fix the initial message
                                message.setAdditionalInfo(currentNumbers[0]+","+currentNumbers[1]);

                                //update the ui
                                mReceiverInterface.updateReceiveSendUI(message);
                                /*if (message == SenderPickDestinationActivity.MESSAGE_OPEN_ACTIVITY) {
                                    //we will open the new activity and wait for the connection via interface
                                    //mReceiverInterface.openNexActivity();
                                }*/
                                //change the action to get ready to receive file
                                mCurrentAction=ACTION_CONFIRM_DETAILS;
                                Log.d(TAG,"We got the details of the file, confirm with sender");

                            } catch (Exception e) {
                                Log.d(TAG, "There is no input stream " + e.getMessage());
                            }
                        }

                        if (mCurrentAction==ACTION_RECEIVE_FILE){
                            //we receive the bytes and then save it
                            Log.d(TAG,"Starting stream of the file");
                            //know the final name of the file
                            String realName=mTextInfoSendObject.getMessageContent();
                            String finalName=new Date().toString()+"-"+realName;

                            //we create the file
                            //fileInputStream=mSocket.getInputStream();

                            byte[] bytes = new byte[16 * 1024];
                            fileOutputStream=new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/" +finalName);
                            BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);

                            Log.d(TAG,"Reading bytes");

                            //initialize progress message
                            String additionalInfo="";

                            //progress message
                            TextInfoSendObject objectUpdate=new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS,realName,additionalInfo);

                            int count;
                            while((count=fileInputStream.read(bytes))>0){
                                bufferedOutputStream.write(bytes,0,count);

                                //set the message
                                //send progress update to UI
                                additionalInfo= mCurrentFile + "," +
                                        mTotalFiles+","+
                                        mCurrentFileSize+","+
                                        String.valueOf(bytes.length);

                                objectUpdate.setAdditionalInfo(additionalInfo);

                                mReceiverInterface.updateReceiveSendUI(objectUpdate);
                            }

                            bufferedOutputStream.flush();
                            bufferedOutputStream.close();
                            fileOutputStream.flush();
                            fileOutputStream.close();

                            Log.d(TAG,"File finished transfer");

                            //store the sent file in the database
                            File tempFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/" +finalName);

                            //get the mime type
                            //we get the mime type
                            Uri uri = Uri.fromFile(tempFile);

                            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                                    .toString());
                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                    fileExtension.toLowerCase());

                            FileListEntry tempEntry=new FileListEntry(tempFile.getAbsolutePath(),
                                    tempFile.getName(),
                                    0,
                                    tempFile.getParent(),
                                    0,
                                    mimeType,
                                    tempFile.isDirectory());

                            //send it
                            mReceiverInterface.updateReceiveReceivedFile(tempEntry);

                            //we store the file
                            //mCurrentAction=ACTION_SEND_MESSAGE;
                            mCurrentAction=ACTION_NEXT_ACTION;
                        }

                    }while (mCurrentAction!=ACTION_NEXT_ACTION);

                    //close the socket
                    if (!mSocket.isClosed()) {
                        try {
                            mSocket.close();
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to close the socket");
                        }
                    }

                    if (!mServerSocket.isClosed()){
                        try{
                            mServerSocket.close();
                        }catch (Exception e){
                            Log.e(TAG, "Failed to close the server socket");
                        }
                    }

                    doWeRepeat=false;
                    mReceiverInterface.finishedReceiveTransfer();
                } catch (Exception e) {
                    Log.d(TAG, "the socket accept has failed, try again");
                    currentSocketRetries++;

                    if (currentSocketRetries==totalSocketRetries) {
                        doWeRepeat=false;
                        mReceiverInterface.socketReceiveFailedClient();
                    }else{
                        try{
                            TimeUnit.SECONDS.sleep(1);
                        }catch (InterruptedException exe){
                            Log.d(TAG,"couldn't interrupt "+exe.getMessage());
                        }
                    }
                }
            }
        }
    }

    //kill the socket
    public Boolean destroy(){
        Log.d(TAG,"Destroy sockets");
        mReceiverInterface=null;
        socketThread.interrupt();

        int bothClosed=0;

        //cancel socket
        if (mSocket!=null) {
            if (mSocket.isClosed()) {
                bothClosed++;
            } else {
                try {
                    mSocket.close();
                    bothClosed++;
                } catch (Exception e) {
                    Log.d(TAG, "Cannot close socket " + e.getMessage());
                }
            }
        }else{
            bothClosed++;
        }

        if (mServerSocket!=null) {
            if (mServerSocket.isClosed()) {
                bothClosed++;
            } else {
                try {
                    mServerSocket.close();
                    bothClosed++;
                } catch (Exception e) {
                    Log.d(TAG, "Cannot close server socket " + e.getMessage());
                }
            }
        }else{
            bothClosed++;
        }

        if (bothClosed==2){
            Log.d(TAG,"Sockets have been destroyed succesfully");
            return true;
        }else{
            Log.d(TAG,"Could not destroy sockets");
            return false;
        }
    }

    public interface ReceiverSocketTransferInterface{
        void updateReceiveSendUI(TextInfoSendObject textInfoSendObject);
        void updateReceiveReceivedFile(FileListEntry fileListEntry);
        void finishedReceiveTransfer();
        void socketReceiveFailedClient();
    }
}