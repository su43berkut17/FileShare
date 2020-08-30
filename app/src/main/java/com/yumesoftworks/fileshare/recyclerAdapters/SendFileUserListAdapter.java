package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.UserSendEntry;

import java.util.List;

public class SendFileUserListAdapter extends RecyclerView.Adapter<SendFileUserListAdapter.UserListViewHolder> {
    private static final String TAG="PickUserAdapter";
    final private SendFileUserListAdapter.ItemClickListener mItemCLickListener;

    private List<UserSendEntry> mUserEntryList;
    private Context mContext;

    public SendFileUserListAdapter (Context context, ItemClickListener listener){
        mContext=context;
        mItemCLickListener=listener;
    }

    @NonNull
    @Override
    public UserListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_rv_pick_user, viewGroup, false);

        return new UserListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserListViewHolder userListViewHolder, int i) {
        // Determine the values of the wanted data
        UserSendEntry userInfoEntry = mUserEntryList.get(i);

        String name=userInfoEntry.getUsername();
        int avatar=userInfoEntry.getAvatar();
        String path;
        //we create the path
        path="avatar_"+String.valueOf(userInfoEntry.getAvatar()+1);

        userListViewHolder.textView.setText(name);

        //Set values in view
        int imageUri = mContext.getResources().getIdentifier(path,"drawable",mContext.getPackageName());
        Glide.with(mContext)
                .load(imageUri)
                .into(userListViewHolder.iv_avatar);
        Log.d(TAG,"path "+path);
    }

    @Override
    public int getItemCount() {
        if (mUserEntryList == null) {
            return 0;
        }
        return mUserEntryList.size();
    }

    //public method to return data in adapter
    public List<UserSendEntry> getUserList(){
        return mUserEntryList;
    }

    //public method to update adapter
    public void setUsers(List<UserSendEntry> userData){
        mUserEntryList=userData;
    }

    //interface
    public interface ItemClickListener{
        void onItemClickListener(int itemId);
    }

    //ViewHolder
    class UserListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textView;
        ImageView iv_avatar;

        public UserListViewHolder(View itemView){
            super(itemView);

            textView=itemView.findViewById(R.id.tv_item_user_name);
            iv_avatar=itemView.findViewById(R.id.iv_item_user_avatar);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemCLickListener.onItemClickListener(getAdapterPosition());
        }
    }
}
