package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private static final String TAG="AvatarAdapter";
    final private ItemClickListener mItemCLickListener;

    private List<AvatarStaticEntry> mAvatarEntryList;
    private Context mContext;
    private int mLastSelected =-1;

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

        //int id=avatarStaticEntry.getId();
        String path=avatarStaticEntry.getPath();
        String type=avatarStaticEntry.getType();


        //Set values in view
        if (type==AvatarStaticEntry.TYPE_LOCAL){
            int imageUri = mContext.getResources().getIdentifier(path,"drawable",mContext.getPackageName());
            Picasso.get().load(imageUri).into(avatarViewHolder.iv_avatar);
            Log.d(TAG,"path "+path);
        }else{
            Picasso.get().load(path).into(avatarViewHolder.iv_avatar);
            Log.d(TAG,"path "+path);
        }

        //set the selected view
        //avatarViewHolder.iv_avatar.setSelected(avatarStaticEntry.getSelected());
        avatarViewHolder.iv_background.setSelected(avatarStaticEntry.getSelected());

        //avatarViewHolder.iv_background_sel.setVisibility(View.INVISIBLE);
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

    //public method to show the selection
    public void setSelectedAvatar(int selectedId){
        Log.i(TAG,"selected id is "+String.valueOf(selectedId)+" the formed id is "+String.valueOf(mLastSelected));

        if (mLastSelected >=0){
            //we unselect the former
            mAvatarEntryList.get(mLastSelected).setSelected(false);
            notifyItemChanged(mLastSelected);
        }
        mAvatarEntryList.get(selectedId).setSelected(true);
        mLastSelected = selectedId;
        notifyItemChanged(selectedId);
        notifyDataSetChanged();
    }

    //public method to get the data from the selectedItem
    public AvatarStaticEntry getSelected(int selId){
        return mAvatarEntryList.get(selId);
    }

    //interface
    public interface ItemClickListener{
        void onItemClickListener(int itemId);
    }

    //ViewHolder
    class AvatarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //
        //TextView textView;
        ImageView iv_avatar;
        FrameLayout iv_background;
        //ImageView iv_background_sel;

        public AvatarViewHolder(View itemView){
            super(itemView);

            //textView=itemView.findViewById(R.id.textView);
            iv_avatar=itemView.findViewById(R.id.iv_avatar_icon);
            iv_background=itemView.findViewById(R.id.iv_avatar_background);
            //iv_background_sel=itemView.findViewById(R.id.iv_avatar_backgroundSel);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemCLickListener.onItemClickListener(getAdapterPosition());
        }
    }
}
