package com.yumesoftworks.fileshare;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListAdapter;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListRecyclerViewItemHelper;

import java.util.List;

public class QueueViewer extends Fragment implements QueueListAdapter.QueueClickListener
        , QueueListRecyclerViewItemHelper.RecyclerViewItemTouchHelperListener
        ,View.OnClickListener{

    //recycler view
    private RecyclerView rvFileQueue;
    private QueueListAdapter rvAdapter;
    private static List<FileListEntry> fileList;

    private QueueFragmentClickListener mQueueClickListener;

    //button
    private Button btnSendFiles;

    public QueueViewer(){
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mQueueClickListener=(QueueFragmentClickListener) context;
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

            //we set the recycler view ite touch helper
            ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new QueueListRecyclerViewItemHelper(0, ItemTouchHelper.RIGHT, this);
            new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvFileQueue);
        //}

        btnSendFiles.setOnClickListener(this);

        return queueView;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mQueueClickListener=null;
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

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        // remove the item from recycler view
        rvAdapter.removeItem(position);
        rvAdapter.notifyItemRemoved(position);

        //rvAdapter.removeItem(viewHolder.getAdapterPosition());
        FileListEntry entryToDelete = rvAdapter.getFileItem(position);

        //remove it from the database
        mQueueClickListener.onItemSwiped(entryToDelete);
    }

    //on click interface
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_fqv_send_files:
                mQueueClickListener.onButtonSendClicked();
        }
    }

    //interface to activity
    public interface QueueFragmentClickListener{
        void onItemSwiped(FileListEntry file);
        void onButtonSendClicked();
    }
}
