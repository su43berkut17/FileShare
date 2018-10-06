package com.yumesoftworks.fileshare.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface FileListDao {
    @Query("SELECT * FROM FileList"+" ORDER BY id")
    LiveData<List<FileListEntry>> loadFileList();

    @Insert
    void insertFile(FileListEntry fileListEntry);

    @Update (onConflict = OnConflictStrategy.REPLACE)
    void updateFile(FileListEntry fileListEntry);

    @Delete
    void deleteFile(FileListEntry fileListEntry);

    @Query("DELETE FROM FileList")
    void clearFileList();
}