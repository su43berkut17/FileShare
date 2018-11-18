package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.SenderPickDestinationActivity;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverPickSocket {
    private static final String TAG="ReceiverPickSocket";

    //local server socket
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private UserInfoEntry mUserInfoEntry;
    //private Boolean mAsyncCanRestart;

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //interface
    private SocketReceiverConnectionInterface mReceiverInterface;

    public ReceiverPickSocket(Context context, ServerSocket serverSocket, UserInfoEntry userInfo){
        mReceiverInterface=(SocketReceiverConnectionInterface) context;
        mServerSocket=serverSocket;
        mUserInfoEntry=userInfo;

        socketHandler=new Handler();
        socketThread=new Thread(new CommunicationThread());
        socketThread.start();
    }

    class CommunicationThread implements Runnable{

        @Override
        public void run() {
            while(true){
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "Async:Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mSocket = mServerSocket.accept();

                    Log.d(TAG, "Async:Sending the user data");
                    try {
                        ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                        messageOut.writeObject(mUserInfoEntry);
                        messageOut.close();
                        Log.d(TAG,"ObjectOutputSteamSent closed");
                    } catch (Exception e) {
                        Log.d(TAG, "Async:There is no output stream " + e.getMessage());
                    }

                    try {
                        ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                        String message = messageIn.readUTF();
                        messageIn.close();
                        Log.d(TAG,"ObjectInputStreamReceived closed");
                        if (message == SenderPickDestinationActivity.MESSAGE_OPEN_ACTIVITY) {
                            //we will open the new activity and wait for the connection via interface
                            mReceiverInterface.openNexActivity();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Async:There is no input stream " + e.getMessage());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Async:the socket accept has failed");
                } finally {
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        Log.d(TAG, "Async:Can't close the socket, this is inside finally");
                    }
                }
            }
        }
    }

    class updateUIThread implements Runnable{

        private String msg;
        public updateUIThread(String message){
            //log the message
            msg=message;
        }

        @Override
        public void run() {
            Log.d(TAG,"UpdateUIThread Message is:"+msg);
        }
    }

    //interface
    public interface SocketReceiverConnectionInterface{
        void openNexActivity();
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
