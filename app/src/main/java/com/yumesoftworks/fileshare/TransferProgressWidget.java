package com.yumesoftworks.fileshare;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class TransferProgressWidget extends AppWidgetProvider {

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
                                int totalNumberOfTransfers,
                                String nameOfCurrentFile,
                                int totalNumberOfFiles,
                                int currentNumberOfFiles) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.transfer_progress_widget);

        //set the values
        mCurrentState=currentState;
        mTotalNumberOfTransfers=totalNumberOfTransfers;
        mNameOfCurrentFile=nameOfCurrentFile;
        mTotalNumberOfFiles=totalNumberOfFiles;
        mCurrentNumberOfFiles=currentNumberOfFiles;

        //depending on the state we hide or show the layouts
        switch (mCurrentState){
            case "INITIAL":
                //We hide the stuff
                views.setViewVisibility(R.id.widget_default_state, View.VISIBLE);
                views.setViewVisibility(R.id.widget_transfer_state, View.GONE);

                //calculate the percentage
                mPercentage=mCurrentNumberOfFiles/mTotalNumberOfFiles;

                //update the texts
                views.setTextViewText(R.id.tv_widget_number_of_transfers,String.valueOf(mTotalNumberOfTransfers));

                //we set the pending intent to launch the main app when the widget is in its default mode
                Intent intent = new Intent(context, WelcomeScreenActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                //we set the pending intent for the widget
                views.setOnClickPendingIntent(R.id.widget_default_state, pendingIntent);

                break;

            case "TRANSFER":
                //we hide the stuff
                views.setViewVisibility(R.id.widget_default_state, View.GONE);
                views.setViewVisibility(R.id.widget_transfer_state, View.VISIBLE);

                //update the texts
                views.setTextViewText(R.id.tv_widget_current_file,mNameOfCurrentFile);
                views.setTextViewText(R.id.tv_widget_number_of_transfers,String.valueOf(mCurrentNumberOfFiles)+" of "+String.valueOf(mTotalNumberOfFiles));

                //update the progress bar
                views.setProgressBar(R.id.pb_widget_progress,1,mPercentage,false);

                //we set the pending intent to launch the main app on transfer mode
                Intent intentT = new Intent(context, TransferProgressActivity.class);
                PendingIntent pendingIntentT = PendingIntent.getActivity(context, 0, intentT, 0);

                //we set the pending intent for the widget
                views.setOnClickPendingIntent(R.id.widget_default_state, pendingIntentT);

                break;
            default:
                //nothing happens
                break;
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, mCurrentState,mTotalNumberOfTransfers,mNameOfCurrentFile,mTotalNumberOfFiles,mCurrentNumberOfFiles);
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