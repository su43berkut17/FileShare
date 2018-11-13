package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.AsyncTask;
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
    private AsyncTaskServer serverSocketTask;
    private UserInfoEntry mUserInfoEntry;

    //interface
    private SocketReceiverConnectionInterface mReceiverInterface;

    public ReceiverPickSocket(Context context, ServerSocket serverSocket, UserInfoEntry userInfo){
        mReceiverInterface=(SocketReceiverConnectionInterface) context;
        mServerSocket=serverSocket;
        mUserInfoEntry=userInfo;
        serverSocketTask=new AsyncTaskServer();
        restartAsyncTask();
    }

    //inerface
    public interface SocketReceiverConnectionInterface{
        void openNexActivity();
    }

    //asynctask that runs the server
    private class AsyncTaskServer extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            //we check as the beggining
            while(this.isCancelled()==false) {
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mSocket = mServerSocket.accept();
                    if (this.isCancelled()) {
                        return null;
                    }

                    Log.d(TAG, "Sending the user data");
                    try {
                        ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                        messageOut.writeObject(mUserInfoEntry);
                    } catch (Exception e) {
                        Log.d(TAG, "There is no output stream " + e.getMessage());
                    }

                    try {
                        ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                        String message = messageIn.readUTF();

                        if (message == SenderPickDestinationActivity.MESSAGE_OPEN_ACTIVITY) {
                            //we will open the new activity and wait for the connection via interface
                            mReceiverInterface.openNexActivity();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "There is no input stream " + e.getMessage());
                    }

                    return null;
                } catch (Exception e) {
                    Log.d(TAG, "the socket accept has failed");
                    return null;
                } finally {
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        Log.d(TAG, "Can't close the socket, this is inside finally");
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG,"OnPostExecute");
            //call the async task again
            restartAsyncTask();
        }
    }

    private void restartAsyncTask(){
        Log.d(TAG,"Executing the async task");
        serverSocketTask.execute();
    }

    //kill the socket
    public void destroySocket(){
        Log.d(TAG,"Trying to close socket");
        try {
            mSocket.close();
        }catch (Exception e){
            Log.d(TAG,"Cannot close socket "+e.getMessage());
        }

        if (serverSocketTask.getStatus()==AsyncTask.Status.RUNNING){
            Log.d(TAG,"Cancelling the task");
            serverSocketTask.cancel(true);
        }
    }
}
