package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    final private ItemClickListener mItemCLickListener;

    private List<AvatarStaticEntry> mAvatarEntryList;
    private Context mContext;

    public AvatarAdapter (Context context, ItemClickListener listener){
        mContext=context;
        mItemCLickListener=listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_rv_avatar, viewGroup, false);

        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder avatarViewHolder, int i) {
        // Determine the values of the wanted data
        AvatarStaticEntry avatarStaticEntry = mAvatarEntryList.get(i);

        int id=avatarStaticEntry.getId();
        String path=avatarStaticEntry.getPath();
        String type=avatarStaticEntry.getType();

        //Set values in view
        //Todo: with picasso load the image in the view or with the drawable value
        //holder.taskDescriptionView.setText(description);
        //holder.updatedAtView.setText(updatedAt);
    }

    @Override
    public int getItemCount() {
        if (mAvatarEntryList == null) {
            return 0;
        }
        return mAvatarEntryList.size();
    }

    //public method to return data in adapter
    public List<AvatarStaticEntry> getAvatarList(){
        return mAvatarEntryList;
    }

    //public method to update adapter
    public void setAvatar(List<AvatarStaticEntry> avatarData){
        mAvatarEntryList=avatarData;
        notifyDataSetChanged();
    }

    //interface
    public interface ItemClickListener{
        void onItemClickListener(int itemId);
    }

    //ViewHolder
    class AvatarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //
        //TextView textView;

        public AvatarViewHolder(View itemView){
            super(itemView);

            //textView=itemView.findViewById(R.id.textView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            ///int elementId=
        }
    }
}
