package com.yumesoftworks.fileshare.utils;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;

public class DiffUtilTransferRecyclerView extends DiffUtil.Callback {
    public final static String DIFF_TRANSFER_STATUS="DIFF_TRANSFER_STATUS";
    List<FileListEntry> updatedList;
    List<FileListEntry> oldList;

    public DiffUtilTransferRecyclerView(List<FileListEntry> updatedList, List<FileListEntry> oldList){
        this.updatedList=updatedList;
        this.oldList=oldList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return updatedList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getPath().equals(updatedList.get(newItemPosition).getPath());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        FileListEntry oldItem=oldList.get(oldItemPosition);
        FileListEntry updatedItem=updatedList.get(newItemPosition);

        return oldItem.getPath().equals(updatedItem.getPath()) && oldItem.getIsTransferred()==updatedItem.getIsTransferred();
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        FileListEntry oldItem=oldList.get(oldItemPosition);
        FileListEntry updatedItem=updatedList.get(newItemPosition);

        Bundle toReturn=new Bundle();
        if (oldItem.getIsTransferred()!=updatedItem.getIsTransferred()) {
            toReturn.putBoolean(DIFF_TRANSFER_STATUS, updatedItem.getIsTransferred() == 1);
            return toReturn;
        }else{
            return super.getChangePayload(oldItemPosition,newItemPosition);
        }
    }
}