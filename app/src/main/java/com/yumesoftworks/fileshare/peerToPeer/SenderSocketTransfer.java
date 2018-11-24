package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SenderSocketTransfer{
    private static final String TAG="SenderSocketTransfer";

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

    //interface
    private SocketSenderConnectionInterface mSenderInterface;

    public SenderSocketTransfer(Context context, String recIpAddress, int recPort){
        mIpAddress=recIpAddress;
        mPort=recPort;
        //mSenderInterface=(SocketSenderConnectionInterface)context;

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

                    Boolean streamLoop=true;
                    Boolean isInitialized=false;

                    while(streamLoop) {
                        // try {
                        if(!isInitialized) {
                            Log.d(TAG, "Object input stream started");
                            ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                            UserInfoEntry readEntry = (UserInfoEntry) messageIn.readObject();
                            //messageIn.close();

                            //set the right data

                            isInitialized=true;

                            //socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_UPDATE, mUserList));
                        }

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
    public interface SocketSenderConnectionInterface{
        //void updateUserDataSocket(UserSendEntry userSendEntry);
    }
}
