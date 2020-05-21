package com.yumesoftworks.fileshare.peerToPeer;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.webkit.MimeTypeMap;

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

public class ReceiverSocketTransfer {
    private static final String TAG="ReceiverSocketTransfer";

    //actions
    private static final int ACTION_SEND_MESSAGE=1001;
    private static final int ACTION_RECEIVE_FILE=1002;
    private static final int ACTION_RECEIVE_DETAILS=1003;
    private static final int ACTION_CONFIRM_DETAILS=1004;
    private static final int ACTION_NEXT_ACTION=1005;
    private static final int ACTION_NOT_ENOUGH_SPACE=1006;
    private static final int ACTION_EXCEPTION=1007;

    //types of next actions
    public static final int NEXT_ACTION_CONTINUE=2001;
    public static final int NEXT_ACTION_CANCEL_SPACE =2002;

    //local server socket
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private int mPort;

    //thread
    //private Handler socketHandler;
    private Thread socketThread;

    //current action
    private int mCurrentAction;
    private int mNextActionDetail;

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

        //socketHandler=new Handler(Looper.getMainLooper());
        socketThread=new Thread(new CommunicationThread());
        socketThread.start();
    }

    class CommunicationThread implements Runnable{
        @Override
        public void run() {
            Boolean doWeRepeat=true;//to retry if needed
            int totalSocketRetries=20;
            int currentSocketRetries=0;

            while(doWeRepeat && !socketThread.isInterrupted()){
                // Socket object
                try {
                    //wait for a connection
                    mServerSocket=new ServerSocket(mPort);
                    mServerSocket.setReuseAddress(true);
                    mServerSocket.setSoTimeout(3000);

                    Log.d(TAG, "Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mSocket = mServerSocket.accept();
                    mSocket.setSoTimeout(3000);

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
                        if (mCurrentAction==ACTION_SEND_MESSAGE || mCurrentAction==ACTION_CONFIRM_DETAILS || mCurrentAction==ACTION_NOT_ENOUGH_SPACE) {
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
                                }else if(mCurrentAction==ACTION_NOT_ENOUGH_SPACE){
                                    Log.d(TAG,"sending error message for not enough space");
                                    TextInfoSendObject textInfoSendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_TRANSFER_NO_SPACE, "", "");
                                    messageOut.writeObject(textInfoSendObject);

                                    //reset action to receive file
                                    mNextActionDetail= NEXT_ACTION_CANCEL_SPACE;
                                    mCurrentAction=ACTION_NEXT_ACTION;
                                }

                            } catch (Exception e) {
                                Log.d(TAG, "Action send mess, confirm deta, not enough spa, There is no output stream " + e.getMessage());
                                mCurrentAction=ACTION_EXCEPTION;
                            }
                        }

                        //receiving
                        if (mCurrentAction==ACTION_RECEIVE_DETAILS){
                            //we read the object
                            try {
                                //messageIn = new ObjectInputStream(mSocket.getInputStream());
                                TextInfoSendObject message = (TextInfoSendObject) messageIn.readObject();
                                mTextInfoSendObject=message;

                                //extract the size of the file and set again
                                String stringNumbers=mTextInfoSendObject.getAdditionalInfo();
                                String[] currentNumbers = stringNumbers.split(",");

                                //Log.d(TAG,"receiving details "+stringNumbers);

                                mCurrentFile=currentNumbers[0];
                                mTotalFiles=currentNumbers[1];
                                mCurrentFileSize=currentNumbers[2];

                                Long currentFileSizeLong=Long.parseLong(mCurrentFile);

                                //fix the initial message
                                message.setAdditionalInfo(currentNumbers[0]+","+currentNumbers[1]);

                                //update the ui
                                mReceiverInterface.updateReceiveSendUI(message);

                                //check if we have enough space available
                                StatFs statfs=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                                Long spaceAvailable;
                                if (Build.VERSION.SDK_INT>=18) {
                                    spaceAvailable=statfs.getAvailableBytes();
                                }else{
                                    spaceAvailable=statfs.getAvailableBlocks()*(long)statfs.getBlockSize();
                                }

                                Log.d(TAG,"Comparing the filesize to the available storage, space available: "+
                                        spaceAvailable.toString()+" file size "+mCurrentFileSize);

                                if (spaceAvailable>currentFileSizeLong) {
                                    //change the action to get ready to receive file
                                    mCurrentAction = ACTION_CONFIRM_DETAILS;
                                    Log.d(TAG, "We got the details of the file, confirm with sender");
                                }else{
                                    //not enough available space
                                    mCurrentAction=ACTION_NOT_ENOUGH_SPACE;
                                    Log.d(TAG, "Error, not enough space");
                                }

                            } catch (Exception e) {
                                Log.d(TAG, "Action receive details, there is no input stream " + e.getMessage());
                                mCurrentAction=ACTION_EXCEPTION;
                            }
                        }

                        if (mCurrentAction==ACTION_RECEIVE_FILE){
                            //we receive the bytes and then save it
                            //Log.d(TAG,"Starting stream of the file");
                            //know the final name of the file
                            String realName=mTextInfoSendObject.getMessageContent();
                            String finalName;
                            String finalExtension;
                            File tempFileName=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + realName);
                            int fileNumber=0;
                            boolean doesFileExist=false;

                            do {
                                //final name
                                //tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + realName);

                                if (tempFileName.exists()) {
                                    //we need to create a new file
                                    fileNumber++;

                                    //separate name and extension so it can be incremented
                                    finalName=realName.substring(0,realName.lastIndexOf("."));
                                    finalExtension=realName.substring(realName.lastIndexOf("."));

                                    tempFileName=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + finalName+"("+fileNumber+")"+finalExtension);
                                }else{
                                    finalName=tempFileName.getName();
                                    doesFileExist=true;
                                }
                            }while(!doesFileExist);
                            //String finalName=new Date().toString()+"-"+realName;

                            //we create the file
                            byte[] bytes = new byte[16 * 1024];
                            fileOutputStream=new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/" +finalName);
                            BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);

                            //Log.d(TAG,"Receiving bytes");

                            //initialize progress message
                            String additionalInfo="";

                            //progress message
                            TextInfoSendObject objectUpdate=new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS,realName,additionalInfo);

                            int count;
                            int byteCounter=0;
                            while((count=fileInputStream.read(bytes))>0){
                                bufferedOutputStream.write(bytes,0,count);

                                //byteCounter+=bytes.length;
                                byteCounter=(int)fileOutputStream.getChannel().size();

                                //set the message
                                //send progress update to UI
                                additionalInfo= mCurrentFile + "," +
                                        mTotalFiles+","+
                                        mCurrentFileSize+","+
                                        String.valueOf(byteCounter);

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
                            mNextActionDetail=NEXT_ACTION_CONTINUE;
                            mCurrentAction=ACTION_NEXT_ACTION;
                        }

                    }while (mCurrentAction!=ACTION_NEXT_ACTION && mCurrentAction!=ACTION_EXCEPTION && !socketThread.isInterrupted());

                    //close the socket
                    if (!mSocket.isClosed()) {
                        try {
                            mSocket.close();
                            Log.e(TAG, "Socket closed");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to close the socket");
                        }
                    }

                    if (!mServerSocket.isClosed()){
                        try{
                            mServerSocket.close();
                            Log.e(TAG, "Server Socket closed");
                        }catch (Exception e){
                            Log.e(TAG, "Failed to close the server socket");
                        }
                    }

                    if (mCurrentAction==ACTION_NEXT_ACTION) {
                        //we finish
                        doWeRepeat = false;
                        mReceiverInterface.finishedReceiveTransfer(mNextActionDetail);
                    }else if (mCurrentAction==ACTION_EXCEPTION){
                        doWeRepeat=false;
                        mReceiverInterface.socketReceiveFailedClient();
                        return;
                    }else{
                        doWeRepeat=false;
                        return;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "the socket accept has failed, try again");
                    currentSocketRetries++;

                    if (currentSocketRetries==totalSocketRetries) {
                        doWeRepeat=false;
                        try{
                            mReceiverInterface.socketReceiveFailedClient();
                        }catch (Exception ie){
                            Log.e(TAG,"Interface Unavailable");
                        }
                        Log.d(TAG, "we ran out of tries for the socket");
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

        int bothClosed=0;

        //cancel socket
        if (mSocket!=null) {
            try {
                mSocket.close();

                if (mSocket.isClosed()) {
                    Log.d(TAG, "Client socket destroyed successfully");
                    bothClosed++;
                }
            } catch (Exception e) {
                Log.d(TAG, "Cannot close receiver socket " + e.getMessage());
            }
        }else{
            bothClosed++;
        }

        if (mServerSocket!=null) {
            try{
                mServerSocket.close();

                if (mServerSocket.isClosed()) {
                    Log.d(TAG, "Server socket destroyed successfully");
                    bothClosed++;
                }
            }catch (Exception e){
                Log.d(TAG, "Cannot close server socket " + e.getMessage());
            }
        }else{
            bothClosed++;
        }

        if (bothClosed==2){
            //socket closed
            mReceiverInterface = null;
            socketThread.interrupt();

            Log.d(TAG,"Sockets have been destroyed successfully");
            return true;
        }else{
            Log.d(TAG,"Could not destroy sockets");
            return false;
        }
    }

    public interface ReceiverSocketTransferInterface{
        void updateReceiveSendUI(TextInfoSendObject textInfoSendObject);
        void updateReceiveReceivedFile(FileListEntry fileListEntry);
        void finishedReceiveTransfer(int typeFinishTransfer);
        void socketReceiveFailedClient();
    }
}