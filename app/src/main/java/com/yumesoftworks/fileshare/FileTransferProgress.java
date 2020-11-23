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
    private TextView mTitleQueue;
    private Button mButton;

    private int mType;

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
        Bundle bundle;
        try {
            bundle=getArguments();
            mType = bundle.getInt(com.yumesoftworks.fileshare.TransferProgressActivity.EXTRA_TYPE_TRANSFER, 0);
        }catch (Exception e){
            bundle=new Bundle();
            bundle.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER,0);
        }

        rvFileList=fileProgressView.findViewById(R.id.rv_file_progress_queue);
        rvFileList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFileList.setHasFixedSize(true);

        rvAdapter=new QueueListAdapter(getContext(),this);

        //we set the adapter
        rvFileList.setAdapter(rvAdapter);
        rvAdapter.notifyDataSetChanged();

        //we get the ui objects
        mTitleQueue=fileProgressView.findViewById(R.id.tv_atp_files_queue);
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
        rvAdapter.setFileListTransfer(receivedFileList);
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
            this.startActivity(Intent.createChooser(myIntent,"Pick a viewer"));
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
