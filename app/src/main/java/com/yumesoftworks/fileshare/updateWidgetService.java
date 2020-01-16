package com.yumesoftworks.fileshare;

import android.appwidget.AppWidgetManager;
import androidx.core.app.JobIntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class updateWidgetService extends JobIntentService {

    private static final String TAG="updateWidgetService";
    public static final String ACTION_UPDATE = "com.yumesoftworks.fileshare.action.APPWIDGET_UPDATE";
    public static final String ACTION_UPDATE_APP = "com.yumesoftworks.fileshare.action.APPWIDGET_UPDATE_APP";
    private static final int JOB_ID = 20671;

    //we create the variables
    private static String mCurrentState;
    private static String mNameOfCurrentFile;
    private static int mTotalNumberOfFiles;
    private static int mCurrentNumberOfFiles;
    private static int mPercentage;

    @Override
    protected void onHandleWork(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                updateWidget("via the normal update");
            }else if (ACTION_UPDATE_APP.equals(action)){
                //this is an update that is done purposely by the app
                updateWidget("purposely via the app");
            }else{
                Log.d(TAG,"FIND OUT HOW IS THIS RUNNING "+intent.getAction());
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

        TransferProgressWidget.updateAllWidgets(this,appWidgetManager,appWidgetsIds, mCurrentState,mNameOfCurrentFile,mTotalNumberOfFiles,mCurrentNumberOfFiles,mPercentage);
    }

    public static void startActionUpdateWidget(Context context,
                                               String recCurrentState,
                                               String recNameOfCurrentFile,
                                               int recTotalNumberOfFiles,
                                               int recCurrentNumberOfFiles,
                                               int percentage){

        //we store the data if it is sent, if it is not the widget doesn't need to update
        if (recCurrentState!=null) {
            mCurrentState=recCurrentState;
            mNameOfCurrentFile=recNameOfCurrentFile;
            mTotalNumberOfFiles=recTotalNumberOfFiles;
            mCurrentNumberOfFiles=recCurrentNumberOfFiles;
            mPercentage=percentage;

            Log.i(TAG,"updates received on the service");
        }else{
            //it is null so it is probably from normal update so we need to read it from the database
            mCurrentState=TransferProgressWidget.STATE_NORMAL;
            mNameOfCurrentFile="";
            mTotalNumberOfFiles=0;
            mCurrentNumberOfFiles=0;
            mPercentage=0;

            //read it from the database
            Log.d(TAG,"update current state is null so it is scheduled update");
        }

        Log.d(TAG,"this is startActionUpdateWidget "+
                mCurrentState+"-"+mNameOfCurrentFile+"-"+
                mTotalNumberOfFiles+" of "+mCurrentNumberOfFiles);

        //we start the intent
        Intent intent=new Intent(context,updateWidgetService.class);
        intent.setAction(ACTION_UPDATE_APP);

        //enqueue work
        enqueueWork(context,updateWidgetService.class,JOB_ID,intent);
    }
}