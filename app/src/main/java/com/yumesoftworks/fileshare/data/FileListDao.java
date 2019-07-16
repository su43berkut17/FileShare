package com.yumesoftworks.fileshare.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FileListDao {
    @Query("SELECT * FROM FileList"+" ORDER BY id")
    LiveData<List<FileListEntry>> loadFileList();

    @Query("SELECT * FROM FileList"+" ORDER BY id")
    //no live data
    List<FileListEntry> loadFileListDirect();

    @Query(("SELECT * FROM FileList WHERE parentFolder= :pathToRead")+" ORDER BY path")
    LiveData<List<FileListEntry>> loadFileListPath(String pathToRead);

    @Insert
    void insertFile(FileListEntry fileListEntry);

    @Update (onConflict = OnConflictStrategy.REPLACE)
    void updateFile(FileListEntry fileListEntry);

    @Delete
    void deleteFile(FileListEntry fileListEntry);

    @Query("DELETE FROM FileList WHERE path = :pathToDelete")
    int deleteFileNotSameId(String pathToDelete);

    @Query("DELETE FROM FileList")
    void clearFileList();
}