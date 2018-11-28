package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SenderPickSocket {
    private static final String TAG="SenderPickSocket";

    //types of message
    private static final String TYPE_UPDATE="typeUpdate";
    private static final String TYPE_END="typeEnd";
    private static final String TYPE_ERROR="typeError";

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //socket
    private Socket mSocket;

    //data
    private String messageToSend;

    //user list
    private UserSendEntry mUserList;

    //interface
    private SocketSenderConnectionInterface mSenderInterface;

    public SenderPickSocket(Context context, UserSendEntry userSendEntry){
        mUserList=userSendEntry;
        mSenderInterface=(SocketSenderConnectionInterface)context;

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
                    Log.d(TAG, "we try to create the socket: " + mUserList.getIpAddress().getHostAddress() + " with port: " + mUserList.getPort());
                    //vars
                    String hostAddress = mUserList.getIpAddress().getHostAddress();
                    int hostIp = mUserList.getPort();

                    mSocket = new Socket(hostAddress, hostIp);

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
                            mUserList.setAvatar(readEntry.getPickedAvatar());
                            mUserList.setUsername(readEntry.getUsername());
                            isInitialized=true;

                            socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_UPDATE, mUserList));
                        }

                        if (messageToSend!=null) {
                            //we send the message
                            try {
                                Log.d(TAG, "we send the message");

                                TextInfoSendObject sendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_END, messageToSend, "");
                                ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                messageOut.writeObject(sendObject);
                                messageToSend = null;
                                doWeRepeat=false;

                                //we open the next activity
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_END, null));
                            }catch (Exception e){
                                Log.d(TAG,"Error sending message: "+messageToSend+" "+e.getMessage());
                                e.printStackTrace();
                                messageToSend=null;
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_ERROR, null));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "the socket creation has failed" + e.getMessage());
                    doWeRepeat=false;
                }
            }
        }
    }

    public void sendMessage(String message){
        messageToSend=message;
        Log.d(TAG,"Object output stream started");
    }

    class updateUIThread implements Runnable{

        private UserSendEntry user;
        private String type;
        public updateUIThread(String recType, UserSendEntry userList){
            //log the message
            user=userList;
            type=recType;
        }

        @Override
        public void run() {
            switch (type){
                case TYPE_UPDATE:
                    Log.d(TAG,"UpdateUIThread Message is:"+user.getUsername());

                    //we send it back to the main activity via interface
                    mSenderInterface.updateUserDataSocket(user);
                    break;

                case TYPE_ERROR:
                    //dialog
                    mSenderInterface.showErrorDialog();
                    break;

                case TYPE_END:
                    //we open the next activity
                    mSenderInterface.openNextActivity(mUserList);
                    break;
            }
        }
    }

    //interface
    public interface SocketSenderConnectionInterface{
        void updateUserDataSocket(UserSendEntry userSendEntry);
        void showErrorDialog();
        void openNextActivity(UserSendEntry userList);
    }

    //kill the socket
    public void destroySocket(){
        //cancel socket
        Log.d(TAG,"Trying to close socket");
        try {
            mSocket.close();
            Log.d(TAG,"socket was closed "+mSocket.isClosed());
        }catch (Exception e){
            Log.d(TAG,"Cannot close socket "+e.getMessage());
        }

        //destroy the thread
        socketThread.interrupt();
        mSocket=null;
        mSenderInterface=null;
    }
}