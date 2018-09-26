package com.yumesoftworks.fileshare.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.util.Log;

@Database(entities = {UserInfoEntry.class},version = 1,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG=AppDatabase.class.getSimpleName();
    private static final Object LOCK=new Object();
    private final static String DATABASE_NAME="fileShare";
    private static AppDatabase sInstance;

    public static AppDatabase getInstance(Context context){
        if (sInstance==null){
            synchronized (LOCK){
                Log.d(TAG,"Creating a new database");
                sInstance= Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, AppDatabase.DATABASE_NAME)
                        .build();
            }
        }
        Log.d(TAG,"We will return the database");
        return sInstance;
    }

    public abstract UserInfoDao userInfoDao();
}
