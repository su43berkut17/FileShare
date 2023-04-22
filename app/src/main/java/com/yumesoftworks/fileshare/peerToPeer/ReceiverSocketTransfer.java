package com.yumesoftworks.fileshare.peerToPeer;

import android.app.usage.StorageStatsManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriPermission;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.yumesoftworks.fileshare.ConstantValues;
import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
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
    private String mCurrentMime;

    //service context if saf
    private Context mContext;

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

    public ReceiverSocketTransfer(Context servContext, TransferFileCoordinatorHelper context, int port){
        mPort=port;
        //mReceiverInterface=(ReceiverSocketTransferInterface) context;
        mReceiverInterface=(ReceiverSocketTransferInterface) context;

        mContext=servContext;

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
                                mTextInfoSendObject = message;

                                //extract the size of the file and set again
                                String stringNumbers = mTextInfoSendObject.getAdditionalInfo();
                                String[] currentNumbers = stringNumbers.split(",");

                                //Log.d(TAG,"receiving details "+stringNumbers);
                                mCurrentFile = currentNumbers[0];
                                mTotalFiles = currentNumbers[1];
                                mCurrentFileSize = currentNumbers[2];
                                mCurrentMime = currentNumbers[3];

                                Long currentFileSizeLong = Long.parseLong(mCurrentFileSize);

                                //fix the initial message
                                message.setAdditionalInfo(currentNumbers[0] + "," + currentNumbers[1]);

                                //update the ui
                                mReceiverInterface.updateReceiveSendUI(message);

                                //check if we have enough space available
                                StatFs statFs;
                                Long spaceAvailable;
                                if (Build.VERSION.SDK_INT >= 31) {
                                    StorageManager manager= (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
                                    spaceAvailable = manager.getAllocatableBytes(manager.getPrimaryStorageVolume().getStorageUuid());
                                } else if (Build.VERSION.SDK_INT >= 18) {
                                    statFs=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                                    spaceAvailable = statFs.getAvailableBytes();
                                } else {
                                    statFs=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                                    spaceAvailable = statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
                                }

                                Log.d(TAG,"Comparing the filesize to the available storage, space available: "+
                                        spaceAvailable.toString()+" file size "+currentFileSizeLong.toString());

                                if (spaceAvailable.compareTo(currentFileSizeLong)>0) {
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
                            String realName = mTextInfoSendObject.getMessageContent();

                            if (Build.VERSION.SDK_INT< ConstantValues.SAF_SDK) {
                                //we receive the bytes and then save it
                                //Log.d(TAG,"Starting stream of the file");
                                //know the final name of the file

                                String finalName;
                                String finalExtension;
                                File tempFileName = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + realName);
                                int fileNumber = 0;
                                boolean doesFileExist = false;

                                do {
                                    //final name
                                    //tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + realName);

                                    if (tempFileName.exists()) {
                                        //we need to create a new file
                                        fileNumber++;

                                        //separate name and extension so it can be incremented
                                        finalName = realName.substring(0, realName.lastIndexOf("."));
                                        finalExtension = realName.substring(realName.lastIndexOf("."));

                                        tempFileName = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + finalName + "(" + fileNumber + ")" + finalExtension);
                                    } else {
                                        finalName = tempFileName.getName();
                                        doesFileExist = true;
                                    }
                                } while (!doesFileExist);
                                //String finalName=new Date().toString()+"-"+realName;

                                //we create the file
                                byte[] bytes = new byte[16 * 1024];
                                fileOutputStream = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + finalName);

                                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                                //initialize progress message
                                String additionalInfo = "";

                                //progress message
                                TextInfoSendObject objectUpdate = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS, realName, additionalInfo);

                                int count;
                                int byteCounter = 0;
                                while ((count = fileInputStream.read(bytes)) > 0) {
                                    bufferedOutputStream.write(bytes, 0, count);

                                    //byteCounter+=bytes.length;
                                    byteCounter = (int) fileOutputStream.getChannel().size();

                                    //set the message
                                    //send progress update to UI
                                    additionalInfo = mCurrentFile + "," +
                                            mTotalFiles + "," +
                                            mCurrentFileSize + "," +
                                            String.valueOf(byteCounter);

                                    objectUpdate.setAdditionalInfo(additionalInfo);

                                    mReceiverInterface.updateReceiveSendUI(objectUpdate);
                                }

                                bufferedOutputStream.flush();
                                bufferedOutputStream.close();
                                fileOutputStream.flush();
                                fileOutputStream.close();

                                Log.d(TAG, "File finished transfer");

                                //store the sent file in the database
                                File tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + finalName);

                                //get the mime type
                                //we get the mime type
                                Uri uri = Uri.fromFile(tempFile);

                                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                                        .toString());
                                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                        fileExtension.toLowerCase());

                                FileListEntry tempEntry = new FileListEntry(tempFile.getAbsolutePath(),
                                        tempFile.getName(),
                                        0,
                                        tempFile.getParent(),
                                        0,
                                        mimeType,
                                        tempFile.isDirectory());

                                //send it
                                mReceiverInterface.updateReceiveReceivedFile(tempEntry);

                                //we store the file
                                mNextActionDetail = NEXT_ACTION_CONTINUE;
                                mCurrentAction = ACTION_NEXT_ACTION;
                            }else{
                                //media store
                                String relativeLocation ;
                                //check the mime type
                                if (mCurrentMime.contains("image")){
                                    relativeLocation=Environment.DIRECTORY_PICTURES+File.separator+"File Share";
                                }else if(mCurrentMime.contains("video")){
                                    relativeLocation=Environment.DIRECTORY_MOVIES+File.separator+"File Share";;
                                }else if(mCurrentMime.contains("audio")){
                                    relativeLocation=Environment.DIRECTORY_MUSIC+File.separator+"File Share";;
                                }else{
                                    relativeLocation=Environment.DIRECTORY_DOWNLOADS+File.separator+"File Share";;
                                }

                                //content values depending on type
                                ContentValues  contentValues = new ContentValues();
                                if (mCurrentMime.contains("image")||mCurrentMime.contains("video")||mCurrentMime.contains("audio")){
                                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, realName);
                                    contentValues.put(MediaStore.MediaColumns.TITLE,realName);
                                    contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED,System.currentTimeMillis()/1000);
                                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mCurrentMime);
                                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);
                                }else{
                                    contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, realName);
                                    contentValues.put(MediaStore.DownloadColumns.TITLE,realName);
                                    contentValues.put(MediaStore.DownloadColumns.DATE_MODIFIED,System.currentTimeMillis()/1000);
                                    contentValues.put(MediaStore.DownloadColumns.MIME_TYPE, mCurrentMime);
                                    contentValues.put(MediaStore.DownloadColumns.RELATIVE_PATH, relativeLocation);
                                }

                                contentValues.put(MediaStore.MediaColumns.IS_PENDING,1);
                                ContentResolver resolver = mContext.getContentResolver();

                                try{
                                    Uri destinationCollectionUri;
                                    if (mCurrentMime.contains("image")){
                                        destinationCollectionUri=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                                    }else if(mCurrentMime.contains("video")){
                                        destinationCollectionUri=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                                    }else if(mCurrentMime.contains("audio")){
                                        destinationCollectionUri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                                    }else{
                                        destinationCollectionUri=MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                                    }

                                    Uri savedFileUri=resolver.insert(destinationCollectionUri,contentValues);

                                    //we create the file
                                    byte[] bytes = new byte[16 * 1024];
                                    OutputStream fileOutputStreamSAF=resolver.openOutputStream(savedFileUri);

                                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStreamSAF);

                                    //initialize progress message
                                    String additionalInfo = "";

                                    //progress message
                                    TextInfoSendObject objectUpdate = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS, realName, additionalInfo);

                                    int count;
                                    //int byteCounter = 0;
                                    Long byteCounter=0L;
                                    while ((count = fileInputStream.read(bytes)) > 0) {
                                        bufferedOutputStream.write(bytes, 0, count);
                                        byteCounter+=bytes.length;

                                        //set the message
                                        //send progress update to UI
                                        additionalInfo = mCurrentFile + "," +
                                                mTotalFiles + "," +
                                                mCurrentFileSize + "," +
                                                String.valueOf(byteCounter);

                                        objectUpdate.setAdditionalInfo(additionalInfo);

                                        mReceiverInterface.updateReceiveSendUI(objectUpdate);
                                    }

                                    bufferedOutputStream.flush();
                                    bufferedOutputStream.close();
                                    fileOutputStreamSAF.flush();
                                    fileOutputStreamSAF.close();

                                    contentValues.clear();

                                    contentValues.put(MediaStore.MediaColumns.IS_PENDING,0);

                                    resolver.update(savedFileUri,contentValues,null,null);

                                    Log.d(TAG, "File finished transfer");

                                    //store the sent file in the database
                                    FileListEntry tempEntry = new FileListEntry(savedFileUri.toString(),
                                            realName,
                                            0,
                                            "",
                                            0,
                                            mCurrentMime,
                                            false);

                                    //send it
                                    mReceiverInterface.updateReceiveReceivedFile(tempEntry);

                                    //we store the file
                                    mNextActionDetail = NEXT_ACTION_CONTINUE;
                                    mCurrentAction = ACTION_NEXT_ACTION;

                                }catch (Exception e){
                                    Log.e(TAG,"Error while saving file");
                                }
                            }
                        }

                    }while (mCurrentAction!=ACTION_NEXT_ACTION && mCurrentAction!=ACTION_EXCEPTION && !socketThread.isInterrupted() && !mSocket.isClosed() && !mServerSocket.isClosed());

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