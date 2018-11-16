package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;

import java.io.ObjectInputStream;
import java.net.Socket;

public class SenderPickSocket {
    private static final String TAG="SenderPickSocket";

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //socket
    private Socket mSocket;

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

    class CommunicationThread  implements Runnable {

        @Override
        public void run() {
            while (true) {
                // block the call until connection is created and return
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "we try to create the socket: " + mUserList.getIpAddress().getHostAddress() + " with port: " + mUserList.getPort());
                    //vars
                    String hostAddress = mUserList.getIpAddress().getHostAddress();
                    int hostIp = mUserList.getPort();

                    //Socket socket = new Socket(mUserList.get(mCurrentSocketItem).getIpAddress().getHostAddress(),mUserList.get(mCurrentSocketItem).getPort());
                    //Socket socket = new Socket(mUserList.get(mCurrentSocketItem).getInfoToSend(),mUserList.get(mCurrentSocketItem).getPort());
                    mSocket = new Socket(hostAddress, hostIp);

                    Log.d(TAG, "Reading the user data");
                    ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                    UserInfoEntry readEntry = (UserInfoEntry) messageIn.readObject();

                    //set the right data
                    mUserList.setAvatar(readEntry.getPickedAvatar());
                    mUserList.setUsername(readEntry.getUsername());
                    mSocket.close();

                    socketHandler.post(new SenderPickSocket.updateUIThread(mUserList));
                } catch (Exception e) {
                    Log.d(TAG, "the socket creation has failed" + e.getMessage());
                }
            }
        }
    }

    class updateUIThread implements Runnable{

        private UserSendEntry user;
        public updateUIThread(UserSendEntry userList){
            //log the message
            user=userList;
        }

        @Override
        public void run() {
            Log.d(TAG,"UpdateUIThread Message is:"+user.getUsername());

            //we send it back to the main activity via interface
            mSenderInterface.updateUserDataSocket(user);
        }
    }

    //interface
    public interface SocketSenderConnectionInterface{
        void updateUserDataSocket(UserSendEntry userSendEntry);
    }

    //kill the socket
    public void destroySocket(){
        //cancel socket
        Log.d(TAG,"Trying to close socket");
        try {
            mSocket.close();
        }catch (Exception e){
            Log.d(TAG,"Cannot close socket "+e.getMessage());
        }

        //destroy the thread
        socketThread.interrupt();
    }
}