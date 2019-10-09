package com.yumesoftworks.fileshare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.StorageListEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.FileListAdapter;
import com.yumesoftworks.fileshare.utils.StorageCheck;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileViewer extends Fragment implements
        FileListAdapter.FileClickListener,
        View.OnClickListener{

    private static final String TAG="FileViewerFrag";
    private static final String RECYCLER_VIEW_POSITION="rvPosition";
    private static final String STORAGE_POSITION="spnPosition";

    //recycler view
    private RecyclerView rvFileList;
    private FileListAdapter rvAdapter;
    private int mRvPosition;
    private LinearLayoutManager mLinearLayoutManager;

    //ui
    private Button btnQueue;
    private Boolean mIsButtonShown;
    private TextView textPath;
    private Spinner storagePicker;

    //spinner
    private ArrayAdapter<StorageListEntry> storageAdapter;
    private String mSelectedStorage="";
    private SpinnerInteractionListener mSpinnerInteractionListener;

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
            mSelectedStorage=savedInstanceState.getString(STORAGE_POSITION);
            Log.d(TAG, "We load the spinner " + mSelectedStorage);
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
        outState.putString(STORAGE_POSITION,mSelectedStorage);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView executing");
        //we check which view to inflate
        View mainView = inflater.inflate(R.layout.fragment_file_viewer, container, false);

        storagePicker=mainView.findViewById(R.id.spi_ffv_storage_list);
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

        rvAdapter = new FileListAdapter(getContext(),this);

        //we set the adapter
        rvFileList.setAdapter(rvAdapter);
        rvAdapter.notifyDataSetChanged();

        //request an update
        mListener.fileFragmentRequestUpdate();

        Log.d(TAG,"the number of items in the adapter is "+rvAdapter.getItemCount());

        //listeners button queue
        btnQueue.setOnClickListener(this);

        //create the spinner
        createSpinner();

        return mainView;
    }

    //spinner creation
    private void createSpinner(){
        List<File> list = new StorageCheck().getStorageList();
        List<StorageListEntry> storageList=new ArrayList<>();

        Log.d(TAG,"creating spinner:");

        //populate
        for (File file:list) {
            String absolutePath=file.getAbsolutePath();
            String absolutePathLower=absolutePath.toLowerCase();

            String name=file.getName()+":";

            if (absolutePathLower.contains("emulated/")){
                name="Internal Storage"+":";
            }

            StorageListEntry entry=new StorageListEntry(name,absolutePath);
            storageList.add(entry);
        }

        storageAdapter=new ArrayAdapter<>(getContext(),
                R.layout.item_spn_content,
                storageList);
        storageAdapter.setDropDownViewResource(R.layout.item_spn_content_dropdown);

        storagePicker.setAdapter(storageAdapter);
        mSpinnerInteractionListener=new SpinnerInteractionListener();

        storagePicker.setOnItemSelectedListener(mSpinnerInteractionListener);
        storagePicker.setOnTouchListener(mSpinnerInteractionListener);
    }

    //custom listener for the spinner
    private class SpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener{
        Boolean isSpinnerTouched=false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d(TAG,"interaction with spinner");
            isSpinnerTouched=true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (isSpinnerTouched) {
                Log.d(TAG, "We have clicked the item " + storageAdapter.getItem(position).getName() + " " +
                        storageAdapter.getItem(position).getPath());
                mListener.fileFragmentSpinner(storageAdapter.getItem(position));
                isSpinnerTouched = false;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    //spinner update selection
    public void updateSpinner(String selectedItem){
        Log.d(TAG,"Update spinner called");
        int selectedIndex=0;

        if (selectedItem!=null && selectedItem.isEmpty()==false){
            for (int i=0;i<storageAdapter.getCount();i++){
                StorageListEntry entry=storageAdapter.getItem(i);
                Log.d(TAG,"Spinner comparing: Selected item: "+selectedItem+" with entry: "+entry.getName()+"-"+entry.getPath());
                if (selectedItem.contains(entry.getPath())){
                //if (entry.getPath().contains(selectedItem)){
                    Log.d(TAG,"IT CONTAINS THE PATH");
                    selectedIndex=i;
                }
            }
        }

        Log.d(TAG,"Setting the selection to index number: "+selectedIndex);
        storagePicker.setSelection(selectedIndex);
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
    public void updatePath(String path,String realPath){
        if (textPath!=null) {
            textPath.setText(path);
            mSelectedStorage=realPath;
            Log.d(TAG,"we update the path to "+mSelectedStorage);
        }
    }

    //update the checkbox
    public void updateCheckbox(FileListEntry recFileListEntry){
        rvAdapter.updateChecklist(recFileListEntry);
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
            mQueueButton = (OnButtonGoToQueueInterface) context;

            Log.d(TAG,"reattaching");
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSpinner(mSelectedStorage);
    }

    @Override
    public void onDetach() {
        Log.d(TAG,"detaching");
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
    public void onItemClickListener(int itemId, int type) {
        if (type==rvAdapter.TYPE_CHECKBOX) {
            onButtonPressed(rvAdapter.getFileItem(itemId));
        }else{
            //check if it is directory
            if (!rvAdapter.getFileItem(itemId).getDirectory()) {
                Intent myIntent = new Intent(Intent.ACTION_VIEW);
                myIntent.setDataAndType(Uri.parse(rvAdapter.getFileItem(itemId).getPath()), rvAdapter.getFileItem(itemId).getMimeType());
                try {
                    this.startActivity(Intent.createChooser(myIntent, "Pick a viewer"));
                } catch (Exception e) {
                    Toast.makeText(getActivity().getBaseContext(), R.string.fb_incompatible_file, Toast.LENGTH_SHORT).show();
                }
            }
        }
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
        void fileFragmentSpinner(StorageListEntry entry);
    }

    public interface OnButtonGoToQueueInterface{
        void onButtonQueueInteraction();
    }
}
