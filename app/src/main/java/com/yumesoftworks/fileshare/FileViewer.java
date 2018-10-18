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
import com.yumesoftworks.fileshare.recyclerAdapters.FileListAdapter;

import java.util.List;

public class FileViewer extends Fragment implements FileListAdapter.FileClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    //recycler view
    private RecyclerView rvFileList;
    private FileListAdapter rvAdapter;
    private static List<FileListEntry> fileList;

    private OnFragmentFileInteractionListener mListener;

    public FileViewer() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FileViewer.
     */
    // TODO: Rename and change types and number of parameters
    public static FileViewer newInstance(String param1, String param2) {
        FileViewer fragment = new FileViewer();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //we check which view to inflate
        View mainView = inflater.inflate(R.layout.fragment_file_viewer, container, false);

        rvFileList=mainView.findViewById(R.id.rv_file_viewer);
        rvFileList.setLayoutManager(new LinearLayoutManager(getContext()));

        //if (fileList != null) {
            rvAdapter = new FileListAdapter(getContext(),this);

            //we set the adapter
            rvFileList.setAdapter(rvAdapter);
            rvAdapter.notifyDataSetChanged();
        //}

        return mainView;
    }

    //update file viewer
    public void updateFileRV(List<FileListEntry> fileListSent){
        rvAdapter.setFileList(fileListSent);
        rvAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentFileInteractionListener) {
            mListener = (OnFragmentFileInteractionListener) context;
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
    }
}