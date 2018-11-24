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

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListAdapter;

import java.util.List;


public class FileTransferSent extends Fragment implements QueueListAdapter.QueueClickListener{

    private OnFragmentInteractionListener mListener;

    //recycler view
    private RecyclerView rvFileList;
    private QueueListAdapter rvAdapter;

    public FileTransferSent() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //create the view
        View fileSentView=inflater.inflate(R.layout.fragment_file_transfer_sent,container,false);

        rvFileList=fileSentView.findViewById(R.id.rv_files_sent);
        rvFileList.setLayoutManager(new LinearLayoutManager(getContext()));

            rvAdapter=new QueueListAdapter(getContext(),this);

            //we set the adapter
            rvFileList.setAdapter(rvAdapter);
            rvAdapter.notifyDataSetChanged();

        return fileSentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteractionSent(uri);
        }
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

    @Override
    public void onQueueClickListener(int itemId) {

    }

    //interface
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteractionSent(Uri uri);
    }
}
