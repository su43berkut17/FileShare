package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.QueueListViewHolder> {
    private static final String TAG="QueueListAdapter";
    final private QueueClickListener mQueueClickListener;

    private List<FileListEntry> mFileList;
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

        //check if it is via saf or file
        if (Build.VERSION.SDK_INT>=21) {
            Uri realURI=Uri.parse(fileListEntry.getPath());
            //realURI=realURI.buildUpon().appendEncodedPath(fileListEntry.getPath()).authority(fileListEntry.getFileName()).build();
            Log.d(TAG,"Uri: "+realURI.toString()+" authority:"+realURI.getAuthority());

            //type
            fileListEntry.setMimeType(mContext.getContentResolver().getType(realURI));

            try{
                DocumentFile file=DocumentFile.fromSingleUri(mContext,realURI);
                fileListEntry.setFileName(file.getName());
                fileSize=file.length();

            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }

        }else{
            File tempFile = new File(fileListEntry.getPath());

            //size
            fileSize = tempFile.length();
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
            if (Build.VERSION.SDK_INT>=21) {
                uri = Uri.parse(fileListEntry.getPath());
            }else{
                uri = Uri.fromFile(new File(fileListEntry.getPath()));
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

    //public method to return data in adapter
    public List<FileListEntry> getAvatarList(){
        return mFileList;
    }

    //public method to update adapter
    public void setFileList(List<FileListEntry> FileListData){
        Log.d(TAG,"settling new file list on the adapter");
        mFileList=FileListData;
        notifyDataSetChanged();
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
        TextView tv_fileName;
        TextView tv_size;
        ConstraintLayout view_foreground;
        FileListEntry fileContents;

        public QueueListViewHolder(View itemView){
            super(itemView);

            iv_icon=itemView.findViewById(R.id.iv_item_file_queue);
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
