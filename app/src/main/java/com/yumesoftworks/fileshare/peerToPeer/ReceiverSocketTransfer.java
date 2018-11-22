package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.SenderPickDestinationActivity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverSocketTransfer {
    private static final String TAG="ServiceClientSocket";

    //local server socket
    private ServerSocket mServerSocket;
    private Socket mSocket;

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //interface
    private ClientSocketTransferInterface mReceiverInterface;

    public ReceiverSocketTransfer(Context context, ServerSocket serverSocketPort){
        mServerSocket = serverSocketPort;
        mReceiverInterface=(ClientSocketTransferInterface) context;

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
                    Log.d(TAG, "Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mSocket = mServerSocket.accept();

                    //loop for sending and receiving

                    Boolean keepLoop=true;
                    while (keepLoop) {

                        //we read the messages sent by sender


                        try {
                            ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                            //messageOut.writeObject(mUserInfoEntry);
                        } catch (Exception e) {
                            Log.d(TAG, "There is no output stream " + e.getMessage());
                        }

                        try {
                            ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                            String message = messageIn.readUTF();

                            if (message == SenderPickDestinationActivity.MESSAGE_OPEN_ACTIVITY) {
                                //we will open the new activity and wait for the connection via interface
                                //mReceiverInterface.openNexActivity();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "There is no input stream " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "the socket accept has failed, try again");
                }
            }
        }
    }

    //inerface
    public interface ClientSocketTransferInterface{
        void finishedProcessClient();
        void socketFailedClient();
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

        //destroy thread
        socketThread.interrupt();
    }
}

