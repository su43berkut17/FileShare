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


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FileTransferProgress.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FileTransferProgress#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileTransferProgress extends Fragment implements QueueListAdapter.QueueClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    //recycler view
    private RecyclerView rvFileList;
    private QueueListAdapter rvAdapter;
    private static List<FileListEntry> fileList;

    public FileTransferProgress() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FileTransferProgress.
     */
    // TODO: Rename and change types and number of parameters
    public static FileTransferProgress newInstance(String param1, String param2) {
        FileTransferProgress fragment = new FileTransferProgress();
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
        //create the view
        View fileProgressView=inflater.inflate(R.layout.fragment_file_transfer_progress, container,false);

        rvFileList=fileProgressView.findViewById(R.id.rv_file_progress_queue);
        rvFileList.setLayoutManager(new LinearLayoutManager(getContext()));

            rvAdapter=new QueueListAdapter(getContext(),this);

            //we set the adapter
            rvFileList.setAdapter(rvAdapter);
            rvAdapter.notifyDataSetChanged();

        return fileProgressView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteractionProgress(uri);
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

    //update the ui
    public void updateData(Bundle bundle){
        //we update the data

    }

    @Override
    public void onQueueClickListener(int itemId) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteractionProgress(Uri uri);
    }
}
