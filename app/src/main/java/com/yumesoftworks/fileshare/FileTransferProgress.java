package com.yumesoftworks.fileshare;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListAdapter;
import com.yumesoftworks.fileshare.TransferProgressActivity;

import java.util.List;


public class FileTransferProgress extends Fragment implements QueueListAdapter.QueueClickListener, View.OnClickListener {

    private OnFragmentInteractionListener mListener;

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

        //get the type via arguments
        Bundle bundle=getArguments();
        mType=bundle.getInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER);

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
        if (mType==TransferProgressActivity.FILES_RECEIVING){
            mTitleQueue.setText(R.string.ats_tv_received_files);
        }else if (mType==TransferProgressActivity.FILES_SENDING){
            mTitleQueue.setText(R.string.atp_tv_files_in_queue);
        }

        //listener of he button
        mButton.setOnClickListener(this);

        return fileProgressView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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
        TextInfoSendObject textInfoSendObject=(TextInfoSendObject) bundle.getSerializable(TransferProgressActivity.ACTION_UPDATE_UI_DATA);

        //name of file, current number and total number
        String fileName=textInfoSendObject.getMessageContent();
        String stringNumbers=textInfoSendObject.getAdditionalInfo();
        String[] currentNumbers = stringNumbers.split(",");
        String finalTextNumbers=currentNumbers[0]+" of "+currentNumbers[1];

        //we change the member variables of the progress
        int totalFiles=Integer.parseInt(currentNumbers[1]);
        int currentFile=Integer.parseInt(currentNumbers[0]);
        int percentage=currentFile*100/totalFiles;

        //we update the data
        mTvFileName.setText(fileName);
        mTvOutOf.setText(finalTextNumbers);
        mtvPercentage.setText(String.valueOf(percentage)+"%");
        mTvProgress.setProgress(percentage);
    }

    public void changeButton(){
        mButton.setText(R.string.gen_button_ok);
    }

    @Override
    public void onQueueClickListener(int itemId) {
        //see if we can open it

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
