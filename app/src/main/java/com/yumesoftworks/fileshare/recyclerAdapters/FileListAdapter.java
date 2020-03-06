package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Guideline;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileListViewHolder> {
    private static final String TAG="FileListAdapter";
    public static final int TYPE_CHECKBOX=1001;
    public static final int TYPE_OPEN_FILE=1002;

    final private FileClickListener mFileClickListener;

    private List<FileListEntry> mFileList;
    private Context mContext;

    public FileListAdapter (Context context, FileClickListener listener){
        mContext=context;
        mFileClickListener=listener;
    }

    @NonNull
    @Override
    public FileListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_rv_file_browser, viewGroup, false);

        return new FileListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileListViewHolder fileListViewHolder, int i) {
        FileListEntry fileListEntry=mFileList.get(i);

        //set values in view
        fileListViewHolder.tv_fileName.setText(fileListEntry.getFileName());

        //we check the checkbox status
        if (fileListEntry.getIsSelected()==1){
            fileListViewHolder.cv_selected.setChecked(true);
        }else{
            fileListViewHolder.cv_selected.setChecked(false);
        }

        //placeholder uri
        int placeholderUri = mContext.getResources().getIdentifier("icon_file_128","drawable",mContext.getPackageName());
        RequestOptions smallSize=new RequestOptions().override(200,200);

        //we check if it is a directory
        if (fileListEntry.getDirectory()!=null && fileListEntry.getDirectory()==true){
            //it is a directory
            fileListViewHolder.cv_selected.setVisibility(View.INVISIBLE);
            int imageUri = mContext.getResources().getIdentifier("icon_folder_128","drawable",mContext.getPackageName());

            Glide.with(mContext)
                    .load(imageUri)
                    .centerCrop()
                    .apply(smallSize)
                    .into(fileListViewHolder.iv_icon);

            fileListViewHolder.tv_date.setVisibility(View.GONE);
            fileListViewHolder.tv_size.setVisibility(View.GONE);
            fileListViewHolder.gd_separator.setGuidelinePercent(1);
        }else{
            //it is a file
            File tempFile = new File(fileListEntry.getPath());

            //date and size
            fileListViewHolder.tv_date.setVisibility(View.VISIBLE);
            fileListViewHolder.tv_size.setVisibility(View.VISIBLE);
            fileListViewHolder.gd_separator.setGuidelinePercent(0.5f);
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
            String finalDate = dateFormat.format(new Date(tempFile.lastModified()));

            //size
            Long fileSize=tempFile.length();
            String sizeUnit;

            if (fileSize>1024*1024){
                //megabytes
                fileSize=fileSize/1024/1024;
                sizeUnit=fileSize+" MB";
            }else{
                //kilobytes
                fileSize=fileSize/1024;
                sizeUnit=fileSize+" KB";
            }

            fileListViewHolder.tv_date.setText(finalDate);
            fileListViewHolder.tv_size.setText(sizeUnit);

            //populate icons
            fileListViewHolder.cv_selected.setVisibility(View.VISIBLE);
            if (fileListEntry.getMimeType()!=null) {
                if (fileListEntry.getMimeType().startsWith("image")) {
                    Uri uri=Uri.fromFile(new File(fileListEntry.getPath()));
                    int tempUri = mContext.getResources().getIdentifier("icon_image_128","drawable",mContext.getPackageName());

                    Glide.with(mContext)
                            .load(uri)
                            .placeholder(tempUri)
                            .centerCrop()
                            .apply(smallSize)
                            .into(fileListViewHolder.iv_icon);
                } else if (fileListEntry.getMimeType().startsWith("video")){
                    Uri uri=Uri.fromFile(new File(fileListEntry.getPath()));
                    int tempUri = mContext.getResources().getIdentifier("icon_video_128","drawable",mContext.getPackageName());

                    Glide.with(mContext)
                            .load(uri)
                            .placeholder(tempUri)
                            .centerCrop()
                            .apply(smallSize)
                            .into(fileListViewHolder.iv_icon);
                }else if (fileListEntry.getMimeType().startsWith("audio")){
                    int tempUri = mContext.getResources().getIdentifier("icon_music_128","drawable",mContext.getPackageName());
                    Glide.with(mContext)
                            .load(tempUri)
                            .centerCrop()
                            .apply(smallSize)
                            .placeholder(placeholderUri)
                            .into(fileListViewHolder.iv_icon);
                }else{
                    int tempUri = mContext.getResources().getIdentifier("icon_file_128","drawable",mContext.getPackageName());
                    Glide.with(mContext)
                            .load(tempUri)
                            .centerCrop()
                            .apply(smallSize)
                            .placeholder(placeholderUri)
                            .into(fileListViewHolder.iv_icon);
                }
            }else{
                //it is a file with no mime type
                int tempUri = mContext.getResources().getIdentifier("icon_file_128","drawable",mContext.getPackageName());
                Glide.with(mContext)
                        .load(tempUri)
                        .centerCrop()
                        .apply(smallSize)
                        .placeholder(placeholderUri)
                        .into(fileListViewHolder.iv_icon);
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
    
    //public method to modify checklist when it is shown in double panels
    public void updateChecklist(FileListEntry fileListEntry){
        //cycle in the file list
        for (FileListEntry file:mFileList){

            if ((file.getPath().equals(fileListEntry.getPath()))&&(file.getFileName().equals(fileListEntry.getFileName()))){
                //same file, we verify the status of the checkbox
                file.setIsSelected(fileListEntry.getIsSelected());
            }
        }

        notifyDataSetChanged();
    }

    //public method to get adapter item
    public FileListEntry getFileItem(int itemId){
        return mFileList.get(itemId);
    }

    //ViewHolder
    class FileListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView iv_icon;
        TextView tv_fileName;
        TextView tv_size;
        TextView tv_date;
        CheckBox cv_selected;
        Guideline gd_separator;
        int test=0;

        public FileListViewHolder(View itemView){
            super(itemView);

            iv_icon=itemView.findViewById(R.id.iv_item_file);
            tv_fileName=itemView.findViewById(R.id.tv_item_file_name);
            cv_selected=itemView.findViewById(R.id.cb_item_file);
            tv_size=itemView.findViewById(R.id.tv_item_file_size);
            tv_date=itemView.findViewById(R.id.tv_item_file_date);
            gd_separator=itemView.findViewById(R.id.guideline);

            //for selector
            cv_selected.setOnClickListener(this);
            itemView.setOnClickListener(this);

            //for image
            iv_icon.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Log.d(TAG,"Click on item "+view.getId());
            //we check it is a thumbnail to open it in browser if needed
            if (view.getId()==R.id.iv_item_file){
                mFileClickListener.onItemClickListener(getAdapterPosition(),TYPE_OPEN_FILE);
            }else {
                //we change the value of selected items
                if (mFileList.get(getAdapterPosition()).getDirectory() == false) {
                    Log.d(TAG, "It is not a directory, it is a " + view.toString());
                    if (mFileList.get(getAdapterPosition()).getIsSelected() == 0) {
                        //we activate the checkbox and values
                        mFileList.get(getAdapterPosition()).setIsSelected(1);

                        if (view.getId() != R.id.cb_item_file) {
                            Log.d(TAG, "Checkbox set to true");
                            cv_selected.setChecked(true);
                        }
                    } else {
                        //we deactivate the checkbox and values
                        mFileList.get(getAdapterPosition()).setIsSelected(0);

                        if (view.getId() != R.id.cb_item_file) {
                            Log.d(TAG, "Checkbox set to FALSE");
                            cv_selected.setChecked(false);
                        }
                    }

                    mFileClickListener.onItemClickListener(getAdapterPosition(),TYPE_CHECKBOX);
                }
            }

            //for navigation only
            if (mFileList.get(getAdapterPosition()).getDirectory()){
                mFileClickListener.onItemClickListener(getAdapterPosition(),TYPE_CHECKBOX);
            }
        }
    }

    //interface
    public interface FileClickListener{
        void onItemClickListener(int itemId,int type);
    }
}