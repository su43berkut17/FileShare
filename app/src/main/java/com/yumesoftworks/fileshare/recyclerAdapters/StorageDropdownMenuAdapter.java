package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yumesoftworks.fileshare.data.StorageListEntry;

import java.util.List;

public class StorageDropdownMenuAdapter extends ArrayAdapter<StorageListEntry> implements Filterable {

    private List<StorageListEntry> objects;

    public StorageDropdownMenuAdapter(@NonNull Context context, int resource, @NonNull final List<StorageListEntry> recObjects) {
        super(context, resource, recObjects);
        objects=recObjects;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Nullable
    @Override
    public StorageListEntry getItem(int position) {
        return objects.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter filterThaDoesNothing=new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results=new FilterResults();
                results.values=objects;
                results.count=objects.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };

        return filterThaDoesNothing;
    }
}
