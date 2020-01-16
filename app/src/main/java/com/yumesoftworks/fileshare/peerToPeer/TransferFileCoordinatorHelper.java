package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.util.Log;

import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.util.List;

public class TransferFileCoordinatorHelper implements SenderSocketTransfer.SenderSocketTransferInterface, ReceiverSocketTransfer.ReceiverSocketTransferInterface{
    private static final String TAG="CoordinatorHelper";
    private static final int TOTAL_LIMIT=999999;

    private Context mContext;

    private List<FileListEntry> mFileList;
    private int mCurrentFile;
    private int mTotalFiles;

    private int typeOfTransfer;

    //port and ip
    private int mPort;
    private String mIpAddress;

    //sockets
    private ReceiverSocketTransfer mReceiverSocketTransfer;
    private SenderSocketTransfer mSenderSocketTransfer;

    //interface
    private ReceiverSocketTransferInterfaceCoor mReceiverInterface;
    private SenderSocketTransferInterfaceCoor mSenderInterface;

    public TransferFileCoordinatorHelper(Context context, String recIpAddress, int recPort, List<FileListEntry> recFileList, int recType){
        mContext=context;
        typeOfTransfer=recType;

        //initalize values
        mFileList=recFileList;
        mCurrentFile=0;
        mTotalFiles = recFileList.size();

        mPort=recPort;
        mIpAddress=recIpAddress;

        mSenderInterface=(SenderSocketTransferInterfaceCoor) mContext;
        mSenderInterface.startedSenderTransfer();

        //we begin the cycle of transfers
        startTransfer();
    }

    public TransferFileCoordinatorHelper(Context context, int recPort, int recType){
        Log.d(TAG,"initializing coordinator helper");
        mContext=context;
        typeOfTransfer=recType;

        Log.d(TAG,"initializing files");
        mCurrentFile=0;
        mTotalFiles=TOTAL_LIMIT;

        Log.d(TAG,"initializing port");
        mPort=recPort;

        mReceiverInterface=(ReceiverSocketTransferInterfaceCoor) mContext;
        mReceiverInterface.startedReceiveTransfer();

        startTransfer();
    }

    public Boolean userCancelled(){
        if (mReceiverSocketTransfer != null) {
            try {
                return mReceiverSocketTransfer.destroy();
            } catch (Exception e) {
                Log.d(TAG, "Couldn't cancel the receiver socket "+e.getMessage());
                return false;
            }
        }

        if (mSenderSocketTransfer!=null) {
            try {
                return mSenderSocketTransfer.destroy();
            } catch (Exception e) {
                Log.d(TAG, "Couldn't cancel the sender socket "+e.getMessage());
                return false;
            }
        }

        return true;
    }

    private void startTransfer(){
        Log.d(TAG,"The total of files "+mTotalFiles+ " the current transfer "+mCurrentFile);
        if (mCurrentFile<mTotalFiles) {
            //we continue with the transfer
            if (typeOfTransfer == TransferProgressActivity.FILES_RECEIVING) {
                Log.d(TAG,"Starting a transfer receiving");
                //create a receiver socket object
                mReceiverSocketTransfer=new ReceiverSocketTransfer(this, mPort);
            } else {
                //create a sender socket object
                Log.d(TAG,"Starting a transfer sending");
                mSenderSocketTransfer=new SenderSocketTransfer(this, mIpAddress, mPort, mFileList.get(mCurrentFile),mCurrentFile,mTotalFiles);
            }
        }else{
            //we finish everything
            if (typeOfTransfer==TransferProgressActivity.FILES_RECEIVING){
                Log.d(TAG,"Finished all the transfers receiving");
                mReceiverInterface.finishedReceiveTransfer();
            }else{
                Log.d(TAG,"Finished all the transfers sending");
                mSenderInterface.finishedSendTransfer();
            }
        }
    }

    //implemented methods from the sender and receiver socket
    //Receiver
    @Override
    public void updateReceiveSendUI(TextInfoSendObject textInfoSendObject) {
        //we update the total files
        if (mTotalFiles==TOTAL_LIMIT){
            String stringNumbers=textInfoSendObject.getAdditionalInfo();
            String[] currentNumbers = stringNumbers.split(",");

            //we change the member variables of the progress
            mTotalFiles=Integer.parseInt(currentNumbers[1]);
        }

        mReceiverInterface.updateReceiveSendUI(textInfoSendObject);
    }

    @Override
    public void updateReceiveReceivedFile(FileListEntry fileListEntry) {
        mReceiverInterface.updateReceivedFile(fileListEntry);
    }

    @Override
    public void finishedReceiveTransfer(int typeOfFinishTransfer) {
        //we check if it is an error or continued transfer
        if (typeOfFinishTransfer==ReceiverSocketTransfer.NEXT_ACTION_CONTINUE) {

            //we finished receiving the object
            mCurrentFile++;

            //we call the counter to add a new transferred file
            mReceiverInterface.addReceivedCounter();

            //destroy socket
            Boolean canWeContinue;
            do {
                canWeContinue = mReceiverSocketTransfer.destroy();
            } while (!canWeContinue);

            //we restart the transfer
            startTransfer();
        }else if(typeOfFinishTransfer==ReceiverSocketTransfer.NEXT_ACTION_CANCEL_SPACE){
            //we ran out of space, cancel transfer and display dialog
            mReceiverInterface.errorReceiveNoSpace();
        }
    }

    @Override
    public void socketReceiveFailedClient() {
        mReceiverInterface.socketReceiveFailedClient();
    }

    //Sender
    @Override
    public void updateSendSendUI(TextInfoSendObject textInfoSendObject) {
        mSenderInterface.updateSendSendUI(textInfoSendObject);
    }

    @Override
    public void finishedSendTransfer(int typeOfFinishedTransfer) {
        if (typeOfFinishedTransfer==SenderSocketTransfer.NEXT_ACTION_CONTINUE) {
            //we finished sending the object
            mCurrentFile++;

            //add the sent counter
            mSenderInterface.addSentCounter();

            //destroy socket
            Boolean canWeContinue = false;
            do {
                canWeContinue = mSenderSocketTransfer.destroy();
            } while (!canWeContinue);

            //we restart the transfer
            startTransfer();
        }else if(typeOfFinishedTransfer==SenderSocketTransfer.NEXT_ACTION_CANCEL_SPACE){
            //receiver ran out of space, cancel the remaining transfers
            mSenderInterface.errorSendNoSpace();
        }
    }

    @Override
    public void updateSendSentFile(FileListEntry fileListEntry) {
        mSenderInterface.updateSendSentFile(fileListEntry);
    }

    @Override
    public void socketErrorSend() {
        mSenderInterface.socketSendFailedClient();
    }

    //interfaces to the service
    public interface SenderSocketTransferInterfaceCoor {
        //void updateUserDataSocket(UserSendEntry userSendEntry);
        void startedSenderTransfer();
        void updateSendSendUI(TextInfoSendObject textInfoSendObject);
        void updateSendSentFile(FileListEntry fileListEntry);
        void addSentCounter();
        void finishedSendTransfer();
        void socketSendFailedClient();
        void errorSendNoSpace();
    }

    public interface ReceiverSocketTransferInterfaceCoor{
        void startedReceiveTransfer();
        void updateReceiveSendUI(TextInfoSendObject textInfoSendObject);
        void updateReceivedFile(FileListEntry fileListEntry);
        void addReceivedCounter();
        void finishedReceiveTransfer();
        void socketReceiveFailedClient();
        void errorReceiveNoSpace();
    }
}