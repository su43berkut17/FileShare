package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class SenderSocketTransfer{
    private static final String TAG="SenderSocketTransfer";

    //actions
    private static final int ACTION_SEND_DETAIL=4001;
    private static final int ACTION_SEND_FILE=4002;
    private static final int ACTION_FINISHED_FILE_TRANSFER=4003;
    private static final int ACTION_WAITING_FILE_SUCCESS=4004;

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //socket
    private Socket mSocket;

    //data
    private String messageToSend;

    //socket info
    private String mIpAddress;
    private int mPort;
    private int mCurrentAction;

    //data handling
    private int mCurrentFile;
    private int mTotalFiles;
    private List<FileListEntry> mFileList;

    //interface
    private SenderSocketTransferInterface mSenderInterface;

    public SenderSocketTransfer(Context context, String recIpAddress, int recPort, List<FileListEntry> recFileList){
        mIpAddress=recIpAddress;
        mPort=recPort;
        mSenderInterface=(SenderSocketTransferInterface)context;
        mFileList=recFileList;
        mCurrentFile=0;
        mTotalFiles=mFileList.size();

        socketHandler=new Handler();
        socketThread=new Thread(new CommunicationThread());
        socketThread.start();
    }

    class CommunicationThread implements Runnable {

        @Override
        public void run() {
            Boolean doWeRepeat=true;

            while (doWeRepeat) {
                // block the call until connection is created and return
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "we try to create the socket: " + mIpAddress + " with port: " + mPort);
                    //vars

                    mSocket = new Socket(mIpAddress, mPort);

                    Log.d(TAG, "Reading the user data");
                    doWeRepeat=false;

                    //we communicate that the transfer by socket has started
                    mSenderInterface.startedSenderTransfer();

                    //we initialize the 1st action
                    mCurrentAction=ACTION_SEND_DETAIL;

                    //initialize streams
                    ObjectOutputStream messageOut=new ObjectOutputStream(mSocket.getOutputStream());
                    ObjectInputStream messageIn=new ObjectInputStream(mSocket.getInputStream());
                    InputStream fileInputStream;
                    OutputStream fileOutputStream=mSocket.getOutputStream();

                    while(mCurrentFile<mTotalFiles) {
                        //we send the 1st file details
                        if (mCurrentAction==ACTION_SEND_DETAIL){
                            //we send the details of the file
                            try {
                                Log.d(TAG, "we send the file details");

                                //we send the file name
                                messageToSend=mFileList.get(mCurrentFile).getFileName();
                                String additionalInfo=String.valueOf(mCurrentFile)+","+String.valueOf(mTotalFiles);

                                TextInfoSendObject sendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS, messageToSend,additionalInfo);
                                //messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                messageOut.writeObject(sendObject);
                                messageOut.flush();
                                mCurrentAction=ACTION_SEND_FILE;

                                //send to ui the current file to be sent
                                mSenderInterface.updateSendSendUI(sendObject);

                            }catch (Exception e){
                                Log.d(TAG,"Error sending file details message: "+messageToSend+" "+e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        //we send the 1st file
                        if (mCurrentAction==ACTION_SEND_FILE){
                            try{
                                Log.d(TAG, "we start sending the file");
                                File file=new File(mFileList.get(mCurrentFile).getPath());
                                // Get the size of the file

                                long length = file.length();
                                Log.d(TAG, "File: getting the length "+length);
                                byte[] bytes = new byte[16 * 1024];
                                fileInputStream = new FileInputStream(file);
                                Log.d(TAG, "File: getting the file input stream "+fileInputStream.toString());
                                //fileOutputStream = mSocket.getOutputStream();

                                int count;
                                while ((count = fileInputStream.read(bytes)) > 0) {
                                    Log.d(TAG, "File: reading the bytes "+count);
                                    fileOutputStream.write(bytes, 0, count);
                                    //Log.d(TAG, "File: wrote the bytes");
                                }

                                fileOutputStream.flush();
                                fileInputStream.close();
                                Log.d(TAG, "File: flushed "+length);

                                //fileOutputStream.close();

                                Log.d(TAG, "File sent");
                                mCurrentAction=ACTION_FINISHED_FILE_TRANSFER;
                            }catch (Exception e){
                                Log.d(TAG,"There was en exception when sending file "+e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        //we check if it is the last file
                        if (mCurrentAction==ACTION_FINISHED_FILE_TRANSFER){
                            //we set the file as transferred in the database
                            mSenderInterface.updateSendSentFile(mFileList.get(mCurrentFile));

                            mCurrentFile++;
                            mCurrentAction=ACTION_WAITING_FILE_SUCCESS;
                        }

                        //we check if it is waiting, we read the object
                        if (mCurrentAction==ACTION_WAITING_FILE_SUCCESS){
                            //we read the object
                            try{
                                //messageIn = new ObjectInputStream(mSocket.getInputStream());
                                TextInfoSendObject message = (TextInfoSendObject) messageIn.readObject();

                                //we check if the message is the success of the file so we can continue with the next file
                                if (message.getMessageType()==TransferProgressActivity.TYPE_FILE_TRANSFER_SUCCESS){
                                    //transfer is completed
                                    Log.d(TAG,"the file has been transferred, we send the next detail");
                                    mCurrentAction=ACTION_SEND_DETAIL;
                                }
                            }catch (Exception e){
                                Log.d(TAG,"Waiting for the file to be stored at destination");
                                //e.printStackTrace();
                            }
                        }

                        // try {

                            //Log.d(TAG, "Object input stream started");
                            //ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                            //UserInfoEntry readEntry = (UserInfoEntry) messageIn.readObject();
                            //messageIn.close();

                            //set the right data



                            //socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_UPDATE, mUserList));


                        /*if (messageToSend!=null) {
                            //we send the message
                            try {
                                Log.d(TAG, "we send the message");

                                TextInfoSendObject sendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_END, messageToSend, "");
                                ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                messageOut.writeObject(sendObject);
                                messageToSend = null;

                                //we open the next activity
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_END, null));
                            }catch (Exception e){
                                Log.d(TAG,"Error sending message: "+messageToSend+" "+e.getMessage());
                                e.printStackTrace();
                                messageToSend=null;
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_ERROR, null));
                            }
                        }*/
                    }
                    //it is over
                    //we show a confirmation dialog
                    mSenderInterface.finishedSendTransfer();

                } catch (Exception e) {
                    Log.d(TAG, "the socket creation has failed" + e.getMessage());
                    //doWeRepeat=false;
                    //mSenderInterface.socketErrorSend();
                }
            }
        }
    }

    //interface
    public interface SenderSocketTransferInterface {
        //void updateUserDataSocket(UserSendEntry userSendEntry);
        void startedSenderTransfer();
        void updateSendSendUI(TextInfoSendObject textInfoSendObject);
        void finishedSendTransfer();
        void updateSendSentFile(FileListEntry fileListEntry);
        void socketErrorSend();
    }
}
