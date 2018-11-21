package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.data.UserSendEntry;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SenderPickSocket {
    private static final String TAG="SenderPickSocket";

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

                    //Socket socket = new Socket(mUserList.get(mCurrentSocketItem).getIpAddress().getHostAddress(),mUserList.get(mCurrentSocketItem).getPort());
                    //Socket socket = new Socket(mUserList.get(mCurrentSocketItem).getInfoToSend(),mUserList.get(mCurrentSocketItem).getPort());
                    mSocket = new Socket(hostAddress, hostIp);

                    Log.d(TAG, "Reading the user data");
                    doWeRepeat=false;

                    Boolean streamLoop=true;
                    Boolean isInitialized=false;

                    while(streamLoop) {
                       // try {
                            if (messageToSend!=null){
                                //we send the message
                                try {
                                    /*ObjectOutputStream messageOut=new ObjectOutputStream(mSocket.getOutputStream());
                                    messageOut.writeUTF(messageToSend);
                                    messageOut.close();*/
                                    Log.d(TAG,"we send the message");
                                    DataOutputStream messageOut=new DataOutputStream(mSocket.getOutputStream());
                                    messageOut.writeUTF(messageToSend);
                                    messageOut.close();
                                    messageToSend=null;
                                    /*OutputStreamWriter writer=new OutputStreamWriter(mSocket.getOutputStream(),"UTF-8");
                                    writer.write(messageToSend,0,messageToSend.length());
                                    messageToSend=null;*/
                                }catch (Exception e){
                                    Log.d(TAG,"No output stream");
                                    messageToSend=null;
                                    //we show the dialog
                                    socketHandler.post(new SenderPickSocket.updateUIThread("dialog",null));
                                }
                            }

                            if(!isInitialized) {
                                Log.d(TAG, "Object input stream started");
                                try {
                                    ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                                    UserInfoEntry readEntry = (UserInfoEntry) messageIn.readObject();

                                    //set the right data
                                    mUserList.setAvatar(readEntry.getPickedAvatar());
                                    mUserList.setUsername(readEntry.getUsername());
                                    isInitialized=true;

                                    socketHandler.post(new SenderPickSocket.updateUIThread("update", mUserList));
                                } catch (Exception e) {
                                    Log.d(TAG, "No input stream");
                                }
                            }

                            //mSocket.close();
                        //} catch (Exception e) {
                           // Log.d(TAG, "InputStream failed " + e.getMessage());
                           // streamLoop=true;
                        //}
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
            if (type == "update") {
                Log.d(TAG,"UpdateUIThread Message is:"+user.getUsername());

                //we send it back to the main activity via interface
                mSenderInterface.updateUserDataSocket(user);
            }else{
                //dialog
                mSenderInterface.showErrorDialog();
            }
        }
    }

    //interface
    public interface SocketSenderConnectionInterface{
        void updateUserDataSocket(UserSendEntry userSendEntry);
        void showErrorDialog();
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