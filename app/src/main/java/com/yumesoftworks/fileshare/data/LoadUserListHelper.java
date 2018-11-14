package com.yumesoftworks.fileshare.data;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class LoadUserListHelper {
    private static final String TAG="LoaderUserListHelper";
    private LoadUserHelperInterface mLoadUserHelperInterface;
    private AppDatabase database;
    private UserInfoEntry mUserInfoEntry;
    private DatabaseAsyncTask mDatabaseAsyncTask;

    public LoadUserListHelper(Context context) {
        mLoadUserHelperInterface = (LoadUserHelperInterface) context;
        //load the database data to be sent (name and number of avatar)
        database=AppDatabase.getInstance(context);
    }

    //create the database
    public void executeAsync(){
        mDatabaseAsyncTask=new DatabaseAsyncTask();
        mDatabaseAsyncTask.execute();
    }

    //class that loads the database
    private class DatabaseAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                Log.d(TAG, "Loading the database");
                mUserInfoEntry = database.userInfoDao().loadUserWidget().get(0);
                return null;
            }else{
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //we initialize the sockets
            Log.d(TAG,"On post execute of the async of the database, we will initialize the sockets");
            mLoadUserHelperInterface.userLoadingFinished(mUserInfoEntry);
        }
    }

    public void destroyTask(){
        mLoadUserHelperInterface=null;
        mDatabaseAsyncTask.cancel(true);
        mDatabaseAsyncTask=null;
    }

    public interface LoadUserHelperInterface{
        void userLoadingFinished(UserInfoEntry userInfoEntry);
    }
}
