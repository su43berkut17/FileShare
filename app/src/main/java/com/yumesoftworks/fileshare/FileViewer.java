package com.yumesoftworks.fileshare;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.FileListAdapter;

import java.util.List;

public class FileViewer extends Fragment implements
        FileListAdapter.FileClickListener,
        View.OnClickListener{

    private static final String TAG="FileViewerFrag";
    private static final String RECYCLER_VIEW_POSITION="rvPosition";

    //recycler view
    private RecyclerView rvFileList;
    private FileListAdapter rvAdapter;
    private static List<FileListEntry> fileList;
    private int mRvPosition;
    private LinearLayoutManager mLinearLayoutManager;

    //ui
    private Button btnQueue;
    private Boolean mIsButtonShown;
    private TextView textPath;

    //interfaces
    private OnFragmentFileInteractionListener mListener;
    private OnButtonGoToQueueInterface mQueueButton;

    public FileViewer() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"on create");
        if (savedInstanceState!=null) {
            mRvPosition = savedInstanceState.getInt(RECYCLER_VIEW_POSITION);
            Log.d(TAG, "We load the position " + mRvPosition);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (rvFileList!=null){
            //Log.d(TAG,"number of items "+rvFileList.getLayoutManager().findFirst());

            mRvPosition=mLinearLayoutManager.findFirstVisibleItemPosition();
            Log.d(TAG,"pausing, we store the value "+mRvPosition);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //mRvPosition=rvFileList.getLayoutManager().onSaveInstanceState();
        mRvPosition=mLinearLayoutManager.findFirstVisibleItemPosition();
        Log.d(TAG,"on save instance state, saving rv "+mRvPosition);
        outState.putInt(RECYCLER_VIEW_POSITION, mRvPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView executing");
        //we check which view to inflate
        View mainView = inflater.inflate(R.layout.fragment_file_viewer, container, false);

        textPath=mainView.findViewById(R.id.tv_ffv_current_path);
        btnQueue=mainView.findViewById(R.id.bt_ffv_review_queue);
        rvFileList=mainView.findViewById(R.id.rv_file_viewer);
        mLinearLayoutManager=new LinearLayoutManager(getContext());
        rvFileList.setLayoutManager(mLinearLayoutManager);

        //we hide the button
        if (mIsButtonShown!=null) {
            if (mIsButtonShown == false) {
                btnQueue.setVisibility(View.GONE);
            }
        }

        //if (fileList != null) {
            rvAdapter = new FileListAdapter(getContext(),this);

            //we set the adapter
            rvFileList.setAdapter(rvAdapter);
            rvAdapter.notifyDataSetChanged();

            //request an update
            mListener.fileFragmentRequestUpdate();

            Log.d(TAG,"the number of items in the adapter is "+rvAdapter.getItemCount());
        //}

            /*if (mRvPosition!=null){
                Log.d(TAG,"we load the position "+mRvPosition.describeContents());
                //rvFileList.getLayoutManager().onRestoreInstanceState(mRvPosition);
                mLinearLayoutManager.onRestoreInstanceState(mRvPosition);
               //mLinearLayoutManager.scrollToPosition(3);
            }*/
        //}

        //listeners button queue
        btnQueue.setOnClickListener(this);

        return mainView;
    }

    //update file viewer
    public void updateFileRV(List<FileListEntry> fileListSent){
        if (rvAdapter!=null) {
            rvAdapter.setFileList(fileListSent);
            rvAdapter.notifyDataSetChanged();
            Log.d(TAG, "update file RV with position " + mRvPosition);
            mLinearLayoutManager.scrollToPosition(mRvPosition);
        }
    }

    //update the path
    public void updatePath(String path){
        textPath.setText(path);
    }

    //hide the button here
    public void hideButton(){
        mIsButtonShown=false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentFileInteractionListener) {
            mListener = (OnFragmentFileInteractionListener) context;
            mQueueButton=(OnButtonGoToQueueInterface) context;
            Log.d(TAG,"reattaching");
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mQueueButton=null;
    }

    //clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_ffv_review_queue:
                //we go to see the queue via the interface to the main activity
                mQueueButton.onButtonQueueInteraction();
                break;
            default:
                break;
        }
    }

    //interfaces from the adapter
    @Override
    public void onItemClickListener(int itemId) {
        onButtonPressed(rvAdapter.getFileItem(itemId));
    }

    //method that takes value from the adapter implementation and sends it to the activity via an interface
    public void onButtonPressed(FileListEntry fileListEntry) {
        if (mListener != null) {
            mListener.onFragmentFileInteraction(fileListEntry);
        }
    }

    //interface to interact with the main activity
    //click on file browser item
    public interface OnFragmentFileInteractionListener {
        void onFragmentFileInteraction(FileListEntry fileItemSelected);
        void fileFragmentRequestUpdate();
    }

    public interface OnButtonGoToQueueInterface{
        void onButtonQueueInteraction();
    }
}
