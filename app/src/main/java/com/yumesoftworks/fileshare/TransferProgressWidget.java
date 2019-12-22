package com.yumesoftworks.fileshare;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.TransferProgressActivity;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class TransferProgressWidget extends AppWidgetProvider {
    //states of widget
    private static final String TAG="AppWidgetProvider";
    public static final String STATE_NORMAL="stateNormal";
    public static final String STATE_TRANSFER="stateTransfer";

    //we create the variables
    private static String mCurrentState;
    private static int mTotalNumberOfTransfers;
    private static String mNameOfCurrentFile;
    private static int mTotalNumberOfFiles;
    private static int mCurrentNumberOfFiles;
    private static int mPercentage;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId,
                                String currentState,
                                String nameOfCurrentFile,
                                int totalNumberOfFiles,
                                int currentNumberOfFiles,
                                int percentage) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.transfer_progress_widget);

        Log.d(TAG,"updateAppWidget starts");
        if (currentState!=null) {
            mCurrentState = currentState;
        }else{
            mCurrentState=STATE_NORMAL;
       }

        Log.d(TAG,"updateAppWidget starts, current state is "+mCurrentState);

        if (mCurrentState.equals(STATE_TRANSFER)){
            mNameOfCurrentFile=nameOfCurrentFile;
            mTotalNumberOfFiles=totalNumberOfFiles;
            mCurrentNumberOfFiles=currentNumberOfFiles;
            mPercentage=percentage;
        }else{
            //we load the database
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                List<UserInfoEntry> listUser = database.userInfoDao().loadUserWidget();
                UserInfoEntry user = listUser.get(0);

                Log.d(TAG, "The number of transfers is " + user + " " + user.getNumberFilesTransferred());
                mTotalNumberOfTransfers = user.getNumberFilesTransferred();
            }catch (Exception e){
                mTotalNumberOfTransfers=0;
            }
        }

        //depending on the state we hide or show the layouts
        switch (mCurrentState){
            case STATE_TRANSFER:
                //we hide the stuff
                views.setViewVisibility(R.id.widget_default_state, View.GONE);
                views.setViewVisibility(R.id.widget_transfer_state, View.VISIBLE);

                //update the texts
                views.setTextViewText(R.id.tv_widget_current_file,mNameOfCurrentFile);
                views.setTextViewText(R.id.tv_widget_number_of_transfers,String.valueOf(mCurrentNumberOfFiles)+" of "+String.valueOf(mTotalNumberOfFiles));

                //calculate the percentage
                /*try {
                    mPercentage = mCurrentNumberOfFiles *100 / mTotalNumberOfFiles;
                }catch (Exception e){
                    mPercentage=0;
                }*/

                //update the progress bar
                views.setProgressBar(R.id.pb_widget_progress,100,mPercentage,false);

                //set the extra
               // Bundle extras=new Bundle();
               // extras.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER,TransferProgressActivity.RELAUNCH_APP);

                //we set the pending intent to launch the main app on transfer mode
                Intent intentTransfer = new Intent(context, TransferProgressActivity.class);
                //intentTransfer.putExtras(extras);

                //PendingIntent pendingIntentTransfer = PendingIntent.getActivity(context, 0, intentTransfer, 0);
                PendingIntent pendingIntentTransfer=PendingIntent.getActivity(context,0,intentTransfer,PendingIntent.FLAG_UPDATE_CURRENT);

                //we set the pending intent for the widget
                views.setOnClickPendingIntent(R.id.widget_transfer_state, pendingIntentTransfer);

                break;
            default:
                //We hide the stuff
                views.setViewVisibility(R.id.widget_default_state, View.VISIBLE);
                views.setViewVisibility(R.id.widget_transfer_state, View.GONE);

                //update the texts
                views.setTextViewText(R.id.tv_widget_total_number,String.valueOf(mTotalNumberOfTransfers));

                //we set the pending intent to launch the main app when the widget is in its default mode
                Intent intent = new Intent(context, MainMenuActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                //we set the pending intent for the widget
                views.setOnClickPendingIntent(R.id.widget_default_state, pendingIntent);

                break;
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        updateWidgetService.startActionUpdateWidget(context,mCurrentState,mNameOfCurrentFile,mTotalNumberOfFiles,mCurrentNumberOfFiles,mPercentage);
    }

    //method that updates all; the widgets since it is only 1 widget for all
    public static void updateAllWidgets(Context context,
                                        AppWidgetManager appWidgetManager,
                                        int[] appWidgetIds,
                                        String currentState,
                                         String nameOfCurrentFile,
                                         int totalNumberOfFiles,
                                         int currentNumberOfFiles,
                                         int percentage){
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, currentState,nameOfCurrentFile,totalNumberOfFiles,currentNumberOfFiles,percentage);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}