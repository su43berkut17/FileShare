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
public interface UserInfoDao {
    @Query("SELECT * FROM userInfo" + " ORDER BY id")
    LiveData<List<UserInfoEntry>> loadUserInfo();

    @Query("SELECT * FROM userInfo" + " ORDER BY id")
    List<UserInfoEntry> loadUserWidget();

    @Insert
    void insertTask(UserInfoEntry userInfoEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateTask(UserInfoEntry userInfoEntry);

    @Delete
    void deleteTask(UserInfoEntry userInfoEntry);

    @Query("DELETE FROM userInfo")
    void emptyTable();
}
