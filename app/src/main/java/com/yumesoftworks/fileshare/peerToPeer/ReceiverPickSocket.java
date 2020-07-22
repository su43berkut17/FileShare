package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.SenderPickDestinationActivity;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ReceiverPickSocket {
    private static final String TAG="ReceiverPickSocket";

    private static final String TYPE_END="typeEnd";
    private static final String TYPE_RESTART="typeRestart";

    //local server socket
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private UserInfoEntry mUserInfoEntry;

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
            Boolean repeatSocketConnection=true;
            while(repeatSocketConnection && !socketThread.isInterrupted()){
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "Async:Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mServerSocket.setSoTimeout(10000);
                    mSocket = mServerSocket.accept();

                    Boolean keepLooping=true;
                    Boolean isInitialized=false;
                    while (keepLooping && !socketThread.isInterrupted()){
                        //on 1st connection we send the data
                        if (!isInitialized){
                            try{
                                Log.d(TAG, "Async:Sending the user data");
                                ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                messageOut.writeObject(mUserInfoEntry);

                                isInitialized=true;
                            }catch (SocketTimeoutException e){
                                Log.d(TAG,"Error:"+e.getMessage()+" try again");
                                isInitialized=false;
                                socketHandler.post(new ReceiverPickSocket.updateUIThread(TYPE_RESTART));
                            }catch (Exception e){
                                Log.d(TAG,"Error:"+e.getMessage()+" try again");
                                isInitialized=false;
                                socketHandler.post(new ReceiverPickSocket.updateUIThread(TYPE_RESTART));
                            }
                        }

                        //Log.d(TAG, "Async: Receiving the user data");
                        try {
                            ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                            TextInfoSendObject message = (TextInfoSendObject) messageIn.readObject();

                            Log.d(TAG, "ObjectInputStreamReceived closed, the message is "+message.getMessageContent());

                            if (message.getMessageContent().equals(SenderPickDestinationActivity.MESSAGE_OPEN_ACTIVITY)) {
                                //we will open the new activity and wait for the connection via interface
                                Log.d(TAG, "We will open the new intent");
                                socketHandler.post(new ReceiverPickSocket.updateUIThread(TYPE_END));

                                keepLooping = false;
                                repeatSocketConnection=false;
                                mSocket.close();
                            }
                        }catch (SocketTimeoutException e){
                            isInitialized=false;
                        }catch (Exception e) {
                            //Log.d(TAG, "Error reading input stream");
                            isInitialized=false;
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Async:the socket accept has failed, trying again "+e.getMessage());
                }
            }
        }
    }

    class updateUIThread implements Runnable{

        private String type;
        public updateUIThread(String message){
            //log the message
            type=message;
        }

        @Override
        public void run() {
            //Log.d(TAG,"UpdateUIThread Message is:"+msg);
            switch (type){
                case TYPE_END:
                    destroySocket();
                    mReceiverInterface.openNexActivity();
                    break;
                case TYPE_RESTART:
                    destroySocket();
                    mReceiverInterface.restartReceiverPickSocket();
                    break;
            }
        }
    }

    //interface
    public interface SocketReceiverConnectionInterface{
        void openNexActivity();
        void restartReceiverPickSocket();
    }

    //kill the socket
    public Boolean destroySocket(){
        //cancel socket
        Log.d(TAG,"Trying to close socket");
        try {
            mSocket.close();
        }catch (Exception e){
            Log.d(TAG,"Cannot close socket "+e.getMessage());
        }

        //cancel server socket
        try{
            mServerSocket.close();
        }catch (Exception e){
            Log.d(TAG,"Cannot close server socket "+e.getMessage());
        }

        //destroy the thread
        socketThread.interrupt();
        mSocket=null;
        mServerSocket=null;

        return true;
    }

    //remove callbacks
    public void removeCallbacks(){
        socketHandler.removeCallbacksAndMessages(null);
        socketHandler=null;
        mReceiverInterface=null;
    }
}