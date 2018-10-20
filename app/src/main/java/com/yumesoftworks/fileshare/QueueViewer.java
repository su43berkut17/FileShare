package com.yumesoftworks.fileshare;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListAdapter;

import java.util.List;

public class QueueViewer extends Fragment implements QueueListAdapter.QueueClickListener{

    //recycler view
    private RecyclerView rvFileQueue;
    private QueueListAdapter rvAdapter;
    private static List<FileListEntry> fileList;

    //button
    private Button btnSendFiles;

    public QueueViewer(){
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //we check which view to inflate
        View queueView = inflater.inflate(R.layout.fragment_queue_viewer, container, false);

        btnSendFiles=queueView.findViewById(R.id.bt_fqv_send_files);
        rvFileQueue=queueView.findViewById(R.id.rv_file_queue);
        rvFileQueue.setLayoutManager(new LinearLayoutManager(getContext()));

        //if (fileList != null) {
            rvAdapter = new QueueListAdapter(getContext(),this);

            //we set the adapter
            rvFileQueue.setAdapter(rvAdapter);
            rvAdapter.notifyDataSetChanged();
        //}

        return queueView;
    }

    //update queue viewer
    public void updateQueue(List<FileListEntry> fileListEntry){
        fileList=fileListEntry;
        rvAdapter.setFileList(fileListEntry);
        rvAdapter.notifyDataSetChanged();
    }

    @Override
    public void onQueueClickListener(int itemId) {

    }
}
