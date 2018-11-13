package com.yumesoftworks.fileshare.peerToPeer;

import android.os.AsyncTask;
import android.util.Log;

import com.yumesoftworks.fileshare.data.UserInfoEntry;

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

    public ReceiverPickSocket(ServerSocket serverSocket, UserInfoEntry userInfo){
        mServerSocket=serverSocket;
        mUserInfoEntry=userInfo;
        serverSocketTask=new AsyncTaskServer();
        restartAsyncTask();
    }

    //asynctask that runs the server
    private class AsyncTaskServer extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            // Socket object
            try {
                //wait for a connection
                Log.d(TAG,"Waiting for the socket to be connected "+mServerSocket.getLocalPort());

                mSocket = mServerSocket.accept();

                Log.d(TAG,"Sending the user data");
                ObjectOutputStream messageOut=new ObjectOutputStream(mSocket.getOutputStream());
                messageOut.writeObject(mUserInfoEntry);

                return null;
            }catch (Exception e) {
                Log.d(TAG, "the socket accept has failed");
                return null;
            }
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
