package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SenderPickSocket {
    private static final String TAG="SenderPickSocket";

    //types of message
    private static final String TYPE_UPDATE="typeUpdate";
    private static final String TYPE_END="typeEnd";
    private static final String TYPE_RESTART_CONNECTION ="typeRestartConnection";
    private static final String TYPE_ERROR_SEND_MESSAGE ="typeErrorMessage";
    private static final String TYPE_ERROR_CONNECTION ="typeErrorConnection";

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

            while (doWeRepeat && !socketThread.isInterrupted()) {
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

                    while(streamLoop && !socketThread.isInterrupted() && !mSocket.isClosed()) {
                       // try {
                        if(!isInitialized) {
                            Log.d(TAG, "Object input stream started");
                            try {
                                ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                                UserInfoEntry readEntry = (UserInfoEntry) messageIn.readObject();

                                //set the right data
                                mUserList.setAvatar(readEntry.getPickedAvatar());
                                mUserList.setUsername(readEntry.getUsername());
                                isInitialized = true;

                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_UPDATE, mUserList));
                            }catch (Exception e){
                                Log.e(TAG,"Couldn't read input stream"+e.getMessage());
                                closeSocket(mSocket);
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_ERROR_SEND_MESSAGE,mUserList));
                            }
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
                                closeSocket(mSocket);
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_END, null));
                            }catch (Exception e){
                                Log.d(TAG,"Error sending message: "+messageToSend+" "+e.getMessage());
                                e.printStackTrace();
                                messageToSend=null;
                                closeSocket(mSocket);
                                socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_ERROR_SEND_MESSAGE, null));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "the socket creation has failed" + e.getMessage());
                    closeSocket(mSocket);
                    socketHandler.post(new SenderPickSocket.updateUIThread(TYPE_ERROR_CONNECTION,null));
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
                case TYPE_RESTART_CONNECTION:
                    Log.d(TAG,"Restating the connection");

                    //we send it back to the main activity via interface
                    mSenderInterface.restartSocketConnection(mSocket,user);
                    break;

                case TYPE_ERROR_SEND_MESSAGE:
                    //dialog
                    mSenderInterface.showErrorDialog();
                    break;

                case TYPE_ERROR_CONNECTION:
                    //dialog
                    mSenderInterface.showConnectionError();
                    break;

                case TYPE_END:
                    //we open the next activity
                    mSenderInterface.openNextActivity(mUserList);
                    break;
            }
        }
    }

    private Boolean closeSocket(Socket socket){
        try {
            mSocket.close();
            return true;
        }catch (Exception e){
            Log.d(TAG,"Cannot close socket "+e.getMessage());
            return false;
        }
    }

    //interface
    public interface SocketSenderConnectionInterface{
        void updateUserDataSocket(UserSendEntry userSendEntry);
        void restartSocketConnection(Socket socket, UserSendEntry userSendEntry);
        void showErrorDialog();
        void showConnectionError();
        void openNextActivity(UserSendEntry userList);
    }

    //kill the socket
    public void destroySocket(){
        //destroy the thread
        socketThread.interrupt();
        mSocket=null;
    }

    //remove callbacks
    public void removeCallbacks(){
        socketHandler.removeCallbacksAndMessages(null);
        socketHandler=null;
        mSenderInterface=null;
    }
}