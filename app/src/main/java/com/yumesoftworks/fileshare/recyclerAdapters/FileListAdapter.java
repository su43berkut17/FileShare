package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileListViewHolder> {
    private static final String TAG="FileListAdapter";
    final private FileClickListener mItemCLickListener;

    private List<FileListEntry> mFileList;
    private Context mContext;

    public FileListAdapter (Context context, FileClickListener listener){
        mContext=context;
        mItemCLickListener=listener;
    }

    @NonNull
    @Override
    public FileListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_tv_file_browser, viewGroup, false);

        return new FileListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileListViewHolder fileListViewHolder, int i) {
        FileListEntry fileListEntry=mFileList.get(i);

        //set values in view
        fileListViewHolder.tv_fileName.setText(fileListEntry.getFileName());

        //TODO: deal with image and MIME type first, maybe have icons for sound and image files
    }

    @Override
    public int getItemCount() {
        if (mFileList == null) {
            return 0;
        }
        return mFileList.size();
    }

    //public method to return data in adapter
    public List<FileListEntry> getAvatarList(){
        return mFileList;
    }

    //public method to update adapter
    public void setFileList(List<FileListEntry> FileListData){
        mFileList=FileListData;
        notifyDataSetChanged();
    }

    //ViewHolder
    class FileListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView iv_icon;
        TextView tv_fileName;
        CheckBox cv_selected;

        public FileListViewHolder(View itemView){
            super(itemView);

            iv_icon=itemView.findViewById(R.id.iv_item_file);
            tv_fileName=itemView.findViewById(R.id.tv_item_file_name);
            cv_selected=itemView.findViewById(R.id.cb_item_file);

            //itemView.setOnClickListener(this);
            cv_selected.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemCLickListener.onItemClickListener(getAdapterPosition());
        }
    }

    //interface
    public interface FileClickListener{
        void onItemClickListener(int itemId);
    }
}