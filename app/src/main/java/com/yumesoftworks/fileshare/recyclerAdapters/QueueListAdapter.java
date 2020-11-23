package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.yumesoftworks.fileshare.ConstantValues;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.utils.DiffUtilTransferRecyclerView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.QueueListViewHolder> {
    private static final String TAG="QueueListAdapter";
    final private QueueClickListener mQueueClickListener;

    private List<FileListEntry> mFileList;
    private Deque<List<FileListEntry>> pendingUpdates = new ArrayDeque<>();
    private Context mContext;

    public QueueListAdapter (Context context, QueueClickListener listener){
        mContext=context;
        mQueueClickListener=listener;
    }

    @NonNull
    @Override
    public QueueListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_rv_file_queue, viewGroup, false);

        return new QueueListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueListViewHolder queueListViewHolder, int i) {
        FileListEntry fileListEntry=mFileList.get(i);
        Long fileSize=0L;

        //check if it has been changed
        if (fileListEntry.getIsTransferred()==1){
            //it is transferred, show the icon
            queueListViewHolder.iv_transferred_icon.setVisibility(View.VISIBLE);
        }else{
            //hide the icon
            queueListViewHolder.iv_transferred_icon.setVisibility(View.GONE);
        }

        //check if it is via saf or file
        if (Build.VERSION.SDK_INT< ConstantValues.SAF_SDK) {
            File tempFile = new File(fileListEntry.getPath());

            //size
            fileSize = tempFile.length();
        }else{
            Uri realURI=Uri.parse(fileListEntry.getPath());

            try{
                DocumentFile file=DocumentFile.fromSingleUri(mContext,realURI);
                fileSize=file.length();

            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }
        }

        String sizeUnit;

        if (fileSize > 1024 * 1024) {
            //megabytes
            fileSize = fileSize / 1024 / 1024;
            sizeUnit = " MB";
        } else {
            //kilobytes
            fileSize = fileSize / 1024;
            sizeUnit = " KB";
        }

        queueListViewHolder.tv_size.setText(fileSize + sizeUnit);

        //date and size
        queueListViewHolder.tv_size.setVisibility(View.VISIBLE);

        //set values in view
        queueListViewHolder.tv_fileName.setText(fileListEntry.getFileName());
        queueListViewHolder.fileContents = fileListEntry;

        Log.d(TAG, "mime type is " + fileListEntry.getMimeType());
        Log.d(TAG, "the path is " + fileListEntry.getPath());

        //placeholder uri
        int placeholderUri = mContext.getResources().getIdentifier("icon_file_128", "drawable", mContext.getPackageName());
        RequestOptions smallSize = new RequestOptions().override(200, 200);

        //it is a file
        if (fileListEntry.getMimeType() != null) {
            Uri uri;

            //get uri
            if (Build.VERSION.SDK_INT<ConstantValues.SAF_SDK) {
                uri = Uri.fromFile(new File(fileListEntry.getPath()));
            }else{
                uri = Uri.parse(fileListEntry.getPath());
            }

            if (fileListEntry.getMimeType().startsWith("image")) {
                int tempUri = mContext.getResources().getIdentifier("icon_image_128", "drawable", mContext.getPackageName());
                Glide.with(mContext)
                        .load(uri)
                        .placeholder(tempUri)
                        .centerCrop()
                        .apply(smallSize)
                        .into(queueListViewHolder.iv_icon);
            } else if (fileListEntry.getMimeType().startsWith("video")) {
                int tempUri = mContext.getResources().getIdentifier("icon_video_128", "drawable", mContext.getPackageName());
                Glide.with(mContext)
                        .load(uri)
                        .placeholder(tempUri)
                        .centerCrop()
                        .apply(smallSize)
                        .into(queueListViewHolder.iv_icon);
            } else if (fileListEntry.getMimeType().startsWith("audio")) {
                int tempUri = mContext.getResources().getIdentifier("icon_music_128", "drawable", mContext.getPackageName());
                Glide.with(mContext)
                        .load(tempUri)
                        .centerCrop()
                        .apply(smallSize)
                        .placeholder(placeholderUri)
                        .into(queueListViewHolder.iv_icon);
            } else {
                int tempUri = mContext.getResources().getIdentifier("icon_file_128", "drawable", mContext.getPackageName());
                Glide.with(mContext)
                        .load(tempUri)
                        .centerCrop()
                        .apply(smallSize)
                        .placeholder(placeholderUri)
                        .into(queueListViewHolder.iv_icon);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mFileList == null) {
            return 0;
        }
        return mFileList.size();
    }

    //public method to update adapter
    public void setFileList(List<FileListEntry> FileListData){
        Log.d(TAG,"settling new file list on the adapter");
        mFileList=FileListData;
        notifyDataSetChanged();
    }

    public void setFileListTransfer(List<FileListEntry> fileListData){
        pendingUpdates.push(fileListData);
        if (pendingUpdates.size()>1){
            return;
        }
        updateItemsTransfer(fileListData);
    }

    void updateItemsTransfer(final List<FileListEntry> newItems) {
        final List<FileListEntry> oldItems = new ArrayList<>(this.mFileList);
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final DiffUtil.DiffResult diffResult =
                        DiffUtil.calculateDiff(new DiffUtilTransferRecyclerView(newItems,oldItems),false);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        applyDiffResult(newItems, diffResult);
                    }
                });
            }
        }).start();
    }

    // This method is called when the background work is done
    protected void applyDiffResult(List<FileListEntry> newItems,
                                   DiffUtil.DiffResult diffResult) {
        pendingUpdates.remove(newItems);
        dispatchUpdates(newItems, diffResult);
        if (pendingUpdates.size()>0){
            List<FileListEntry> latest = pendingUpdates.pop();
            pendingUpdates.clear();
            updateItemsTransfer(latest);
        }
    }

    //Finally notify adapter
    protected void dispatchUpdates(List<FileListEntry> newItems,
                                   DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(this);
        mFileList.clear();
        mFileList.addAll(newItems);
    }

    //public method to get adapter item
    public FileListEntry getFileItem(int itemId){
        return mFileList.get(itemId);
    }

    //methods for deleting and undoing delete items
    public void removeItem(int position) {
        mFileList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position,getItemCount());
    }

    public void restoreItem(FileListEntry item, int position) {
        mFileList.add(position, item);
        notifyItemInserted(position);
    }

    //ViewHolder
    class QueueListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView iv_icon;
        ImageView iv_transferred_icon;
        TextView tv_fileName;
        TextView tv_size;
        ConstraintLayout view_foreground;
        FileListEntry fileContents;

        public QueueListViewHolder(View itemView){
            super(itemView);

            iv_icon=itemView.findViewById(R.id.iv_item_file_queue);
            iv_transferred_icon=itemView.findViewById(R.id.iv_item_transferred);
            tv_fileName=itemView.findViewById(R.id.tv_item_file_name_queue);
            view_foreground=itemView.findViewById(R.id.v_item_file_queue_foreground);
            tv_size=itemView.findViewById(R.id.tv_item_file_size_queue);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //we change the value of selected items
            if (mFileList.get(getAdapterPosition()).getIsSelected()==0){
                //we activate the checkbox
                mFileList.get(getAdapterPosition()).setIsSelected(1);

            }else{
                //we deactivate the checkbox and values
                mFileList.get(getAdapterPosition()).setIsSelected(0);
            }

            mQueueClickListener.onQueueClickListener(getAdapterPosition());
        }
    }

    //interface
    public interface QueueClickListener{
        void onQueueClickListener(int itemId);
    }
}
