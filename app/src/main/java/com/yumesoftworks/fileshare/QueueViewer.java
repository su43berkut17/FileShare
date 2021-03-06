package com.yumesoftworks.fileshare;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListAdapter;
import com.yumesoftworks.fileshare.recyclerAdapters.QueueListRecyclerViewItemHelper;

import java.util.List;

public class QueueViewer extends Fragment implements QueueListAdapter.QueueClickListener
        , QueueListRecyclerViewItemHelper.RecyclerViewItemTouchHelperListener
        ,View.OnClickListener{

    private static final String RECYCLER_VIEW_POSITION="rvPosition";

    //recycler view
    private RecyclerView rvFileQueue;
    private QueueListAdapter rvAdapter;
    private static List<FileListEntry> fileList;
    private int mRvPosition;
    private LinearLayoutManager mLinearLayoutManager;
    private LinearLayout mEmptyList;

    private QueueFragmentClickListener mQueueClickListener;

    //button
    private Button btnSendFiles;

    public QueueViewer(){
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mQueueClickListener=(QueueFragmentClickListener) context;

        if (rvAdapter!=null) {
            if (rvAdapter.getItemCount() > 0) {
                mEmptyList.setVisibility(View.INVISIBLE);
            } else {
                mEmptyList.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mRvPosition=savedInstanceState.getInt(RECYCLER_VIEW_POSITION);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (rvFileQueue!=null){
            mRvPosition=mLinearLayoutManager.findFirstVisibleItemPosition();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (rvFileQueue!=null){
            mRvPosition=mLinearLayoutManager.findFirstVisibleItemPosition();
        }

        outState.putInt(RECYCLER_VIEW_POSITION,mRvPosition);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //we check which view to inflate
        View queueView = inflater.inflate(R.layout.fragment_queue_viewer, container, false);

        btnSendFiles=queueView.findViewById(R.id.bt_fqv_send_files);
        rvFileQueue=queueView.findViewById(R.id.rv_file_queue);
        mLinearLayoutManager=new LinearLayoutManager(getContext());
        rvFileQueue.setLayoutManager(mLinearLayoutManager);
        mEmptyList=queueView.findViewById(R.id.ll_fqv_empty);

        //if (fileList != null) {
            rvAdapter = new QueueListAdapter(getContext(),this);

            //we set the adapter
            rvFileQueue.setAdapter(rvAdapter);
            rvAdapter.notifyDataSetChanged();

            //request an update
            mQueueClickListener.queueFragmentRequestUpdate();

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
        if (rvAdapter!=null) {
            fileList = fileListEntry;
            rvAdapter.setFileList(fileListEntry);
            rvAdapter.notifyDataSetChanged();
            mLinearLayoutManager.scrollToPosition(mRvPosition);

            if (rvAdapter.getItemCount()>0){
                mEmptyList.setVisibility(View.INVISIBLE);
            }else{
                mEmptyList.setVisibility(View.VISIBLE);
            }
        }else{
            mEmptyList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onQueueClickListener(int itemId) {
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        FileListEntry entryToDelete = rvAdapter.getFileItem(position);

        // remove the item from recycler view
        rvAdapter.removeItem(position);
        rvAdapter.notifyItemRemoved(position);

        Boolean isLast;

        if (rvAdapter.getItemCount()<=0){
            isLast=true;
        }else{
            isLast=false;
        }

        //remove it from the database
        mQueueClickListener.onItemSwiped(entryToDelete, isLast);
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
        void onItemSwiped(FileListEntry file, Boolean isLast);
        void onButtonSendClicked();
        void queueFragmentRequestUpdate();
    }
}
