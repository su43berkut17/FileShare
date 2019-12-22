package com.yumesoftworks.fileshare.data;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

@Database(entities = {UserInfoEntry.class, FileListEntry.class},version = 6,exportSchema = false)
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
                        .addMigrations(MIGRATION_1_2
                                ,MIGRATION_2_3
                                ,MIGRATION_3_4
                                ,MIGRATION_4_5
                                ,MIGRATION_5_6)
                        .build();
            }
        }
        Log.d(TAG,"We will return the database");
        return sInstance;
    }

    public abstract UserInfoDao userInfoDao();
    public abstract FileListDao fileListDao();

    static final Migration MIGRATION_1_2=new Migration(1,2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //delete old table
            database.execSQL("DROP TABLE userInfo");

            //create new table
            database.execSQL("CREATE TABLE userInfo (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT, pickedAvatar INTEGER NOT NULL, numberFilesTransferred INTEGER NOT NULL, assetVersion INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_2_3=new Migration(2,3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE FileList (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, path TEXT, fileName TEXT, isTransferred INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_3_4=new Migration(3,4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //delete old file entry table
            database.execSQL("DROP TABLE FileList");

            //create new table
            database.execSQL("CREATE TABLE FileList (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, path TEXT, fileName TEXT, isTransferred INTEGER NOT NULL, parentFolder TEXT, isSelected INTEGER NOT NULL, mimeType TEXT)");
        }
    };

    static final Migration MIGRATION_4_5=new Migration(4,5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //delete old user table
            database.execSQL("DROP TABLE userInfo");

            //create new table
            database.execSQL("CREATE TABLE userInfo (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT, pickedAvatar INTEGER NOT NULL, numberFilesTransferred INTEGER NOT NULL, assetVersion INTEGER NOT NULL, isTransferInProgress INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_5_6=new Migration(5,6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //delete old user table
            database.execSQL("DROP TABLE userInfo");

            //create new table
            database.execSQL("CREATE TABLE userInfo (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT, pickedAvatar INTEGER NOT NULL, numberFilesTransferred INTEGER NOT NULL, assetVersion INTEGER NOT NULL, isTransferInProgress INTEGER NOT NULL, transferTypeSendOrReceive INTEGER NOT NULL)");
        }
    };
}
