package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

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
    private static final int ACTION_SEND_FILE=4001;
    private static final int ACTION_FINISHED_FILE_TRANSFER=4001;

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
                                ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                messageOut.writeObject(sendObject);
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
                            File file=new File(mFileList.get(mCurrentFile).getPath());
                            // Get the size of the file
                            long length = file.length();
                            byte[] bytes = new byte[16 * 1024];
                            InputStream in = new FileInputStream(file);
                            OutputStream out = mSocket.getOutputStream();

                            int count;
                            while ((count = in.read(bytes)) > 0) {
                                out.write(bytes, 0, count);
                            }

                            out.close();

                            mCurrentAction=ACTION_FINISHED_FILE_TRANSFER;
                        }

                        //we check if it is the last file
                        if (mCurrentAction==ACTION_FINISHED_FILE_TRANSFER){
                            mCurrentFile++;
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
                } catch (Exception e) {
                    Log.d(TAG, "the socket creation has failed" + e.getMessage());
                    doWeRepeat=false;
                }
            }
        }
    }

    //interface
    public interface SenderSocketTransferInterface {
        //void updateUserDataSocket(UserSendEntry userSendEntry);
        void startedSenderTransfer();
        void updateSendSendUI(TextInfoSendObject textInfoSendObject);
    }
}
