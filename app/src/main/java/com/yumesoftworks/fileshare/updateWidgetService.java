package com.yumesoftworks.fileshare;

import android.app.IntentService;
import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.arch.persistence.room.Database;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoDao;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

public class updateWidgetService extends IntentService {

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String TAG="WidgetUpdaterService";
    public static final String ACTION_UPDATE = "com.yumesoftworks.fileshare.action.APPWIDGET_UPDATE";
    public static final String ACTION_UPDATE_APP = "com.yumesoftworks.fileshare.action.APPWIDGET_UPDATE_APP";

    //we create the variables
    private static String mCurrentState;
    private static int mTotalNumberOfTransfers;
    private static String mNameOfCurrentFile;
    private static int mTotalNumberOfFiles;
    private static int mCurrentNumberOfFiles;

    public updateWidgetService() {
        super("updateWidgetService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1,new Notification());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                updateWidget("via the normal update");
            }else if (ACTION_UPDATE_APP.equals(action)){
                //this is an update that is done purposely by the app
                updateWidget("purposely via the app");
            }else{
                Log.d(TAG,"FIND OUT HOW IS THIS RUNNING");
            }
        }
    }

    //method to update the widget
    private void updateWidget(String recOrigin){
        //we check the origin of the update
        Log.d(TAG,"We received the update from "+recOrigin);

        //data has been sent, we update the widget!
        //we get the widget manager
        AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(this);
        int[] appWidgetsIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,TransferProgressWidget.class));

        TransferProgressWidget.updateAllWidgets(this,appWidgetManager,appWidgetsIds, mCurrentState,mTotalNumberOfTransfers,mNameOfCurrentFile,mTotalNumberOfFiles,mCurrentNumberOfFiles);
    }

    public static void startActionUpdateWidget(Context context,String recCurrentState,
                                               int recTotalNumberOfTransfers,
                                               String recNameOfCurrentFile,
                                               int recTotalNumberOfFiles,
                                               int recCurrentNumberOfFiles){

        //we store the data if it is sent, if it is not the widget doesn't need to update
        Log.d(TAG,"this is startActionUpdateWidget");

        if (recCurrentState!=null) {
            mCurrentState=recCurrentState;
            mTotalNumberOfTransfers=recTotalNumberOfTransfers;
            mNameOfCurrentFile=recNameOfCurrentFile;
            mTotalNumberOfFiles=recTotalNumberOfFiles;
            mCurrentNumberOfFiles=recCurrentNumberOfFiles;

            Log.i(TAG,"updates received on the service");
        }else{
            //it is null so it is probably from normal update so we need to read it from the database
            mCurrentState="INITIAL_STATE";

            //read it from the database
            Log.d(TAG,"update current state is null so it is scheduled update");
        }

        //we start the intent
        Intent intent=new Intent(context,updateWidgetService.class);
        intent.setAction(ACTION_UPDATE_APP);

        //we check if it is before android O
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            context.startForegroundService(intent);
        }else {
            context.startService(intent);
        }
    }
}