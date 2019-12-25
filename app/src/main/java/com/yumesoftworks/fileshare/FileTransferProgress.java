package com.yumesoftworks.fileshare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListAdapter;

import java.util.List;

public class FileTransferProgress extends Fragment implements QueueListAdapter.QueueClickListener, View.OnClickListener {

    private OnFragmentInteractionListener mListener;

    private static final String TAG="FileTransferProgress";

    //recycler view
    private RecyclerView rvFileList;
    private QueueListAdapter rvAdapter;

    //ui
    private TextView mTvFileName;
    private TextView mTvOutOf;
    private TextView mtvPercentage;
    private ProgressBar mTvProgress;
    private TextView mTitleQueue;
    private Button mButton;

    private int mType;
    private int mContinuousPercentage;

    public FileTransferProgress(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //create the view
        View fileProgressView=inflater.inflate(R.layout.fragment_file_transfer_progress, container,false);

        Log.d(TAG,"Creating the views for the fragment");

        //get the type via arguments
        Bundle bundle=getArguments();
        mType=bundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.EXTRA_TYPE_TRANSFER);

        rvFileList=fileProgressView.findViewById(R.id.rv_file_progress_queue);
        rvFileList.setLayoutManager(new LinearLayoutManager(getContext()));

        mTitleQueue=fileProgressView.findViewById(R.id.tv_atp_files_queue);

        rvAdapter=new QueueListAdapter(getContext(),this);

        //we set the adapter
        rvFileList.setAdapter(rvAdapter);
        rvAdapter.notifyDataSetChanged();

        //we get the ui objects
        mTvFileName=fileProgressView.findViewById(R.id.tv_atp_filename);
        mTvOutOf=fileProgressView.findViewById(R.id.tv_atp_files_out_of);
        mtvPercentage=fileProgressView.findViewById(R.id.tv_atp_percentage);
        mTvProgress=fileProgressView.findViewById(R.id.pro_bar_atp);
        mButton=fileProgressView.findViewById(R.id.btn_atp_cancelOk);

        //get the text depending on the type
        transferType(mType);

        //listener of he button
        mButton.setOnClickListener(this);

        Log.d(TAG,"Creating the views for the fragment COMPLETED");

        return fileProgressView;
    }

    //change type of file
    public void transferType(int typeOfTransfer){
        if (typeOfTransfer == com.yumesoftworks.fileshare.TransferProgressActivity.FILES_RECEIVING){
            mTitleQueue.setText(R.string.ats_tv_received_files);
        }else if (typeOfTransfer == com.yumesoftworks.fileshare.TransferProgressActivity.FILES_SENDING){
            mTitleQueue.setText(R.string.atp_tv_files_in_queue);
        }else{
            mTitleQueue.setText(R.string.ats_tv_error);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            Log.d(TAG,"Added the interface on attach");
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //updating recycler view
    public void updateRV(List<FileListEntry> receivedFileList){
        rvAdapter.setFileList(receivedFileList);
        rvAdapter.notifyDataSetChanged();
    }

    //update the ui
    public void updateData(Bundle bundle){
        //process data
        TextInfoSendObject textInfoSendObject=(TextInfoSendObject) bundle.getSerializable(com.yumesoftworks.fileshare.TransferProgressActivity.ACTION_UPDATE_UI_DATA);

        try {
            //name of file, current number and total number
            String fileName = textInfoSendObject.getMessageContent();
            String stringNumbers = textInfoSendObject.getAdditionalInfo();
            String[] currentNumbers = stringNumbers.split(",");
            String finalTextNumbers = currentNumbers[0] + " of " + currentNumbers[1];

            //we change the member variables of the progress
            //int currentFile = Integer.parseInt(currentNumbers[0]);
            //int totalFiles = Integer.parseInt(currentNumbers[1]);
            int percentage=0;
            if (currentNumbers.length>2) {
                percentage = Integer.parseInt(currentNumbers[2]);
            }
            /*int percentage = currentFile * 100 / totalFiles;

            //if this is the percentage of bytes
            if (currentNumbers.length > 3) {
                //percentage based on the bytes sent
                long totalBytes = Long.parseLong(currentNumbers[2]);
                long currentBytes = Long.parseLong(currentNumbers[3]);
                long percentageBytes = currentBytes * 100 / totalBytes;
                int percentageBytesInt = (int) percentageBytes;

                if (percentageBytesInt > 100) {
                    percentageBytesInt = 100;
                }

                //percentage based on the total
                int singlePercentage = 100 / totalFiles;

                //final percentage
                percentage = percentage + (percentageBytesInt * singlePercentage / 100);
            }*/
            if (mContinuousPercentage !=percentage && percentage<=100 && percentage>=1){
                mContinuousPercentage = percentage;
            }

            //we update the data
            mTvFileName.setText(fileName);
            mTvOutOf.setText(finalTextNumbers);
            mtvPercentage.setText(String.valueOf(mContinuousPercentage) + "%");
            mTvProgress.setProgress(mContinuousPercentage);
        }catch (Exception e){
            Log.e(TAG,"There was an exception while updating UI "+e.getMessage());
            mTvFileName.setText("--");
            mTvOutOf.setText("--");
            mtvPercentage.setText("0%");
            mTvProgress.setProgress(0);
        }
    }

    //update ui completed
    public void setComplete(){
        Log.d(TAG,"Called on complete method to update to success");
        mTvFileName.setText(R.string.service_success);
        mTvOutOf.setText("");
        mtvPercentage.setText("100%");
        mTvProgress.setProgress(100);
    }

    public void changeButton(){
        mButton.setText(R.string.gen_button_ok);
    }

    @Override
    public void onQueueClickListener(int itemId) {
        //open file if type compatible
        Intent myIntent = new Intent(Intent.ACTION_VIEW);
        myIntent.setDataAndType(Uri.parse(rvAdapter.getFileItem(itemId).getPath()),rvAdapter.getFileItem(itemId).getMimeType());
        try {
            this.startActivity(myIntent.createChooser(myIntent,"Pick a viewer"));
        }catch (Exception e){
            Toast.makeText(getActivity().getBaseContext(), R.string.fb_incompatible_file, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_atp_cancelOk:
                //we exit
                mListener.buttonOkCancel(mButton.getText().toString());
                break;
            default:
                break;
        }
    }

    //interface
    public interface OnFragmentInteractionListener {
        void buttonOkCancel(String text);
    }
}
