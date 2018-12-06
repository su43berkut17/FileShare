package com.yumesoftworks.fileshare;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import com.yumesoftworks.fileshare.FileBrowserAndQueueActivity;
import com.yumesoftworks.fileshare.ReceiverPickDestinationActivity;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener{
    //buttons
    ConstraintLayout sendFilesButton;
    ConstraintLayout receiveFilesButton;

    //view model
    private FileViewerViewModel fileViewerViewModel;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_main_menu);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //we set the values of the constraint layouts
        sendFilesButton=(ConstraintLayout)findViewById(R.id.mm_surf_sendFileArea);
        receiveFilesButton=(ConstraintLayout)findViewById(R.id.mm_surf_receiveArea);

        //we set the click listeners on the buttons
        sendFilesButton.setOnClickListener(this);
        receiveFilesButton.setOnClickListener(this);

        //we empty the stored database
        fileViewerViewModel= ViewModelProviders.of(this).get(FileViewerViewModel.class);
        fileViewerViewModel.deleteTable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_options:
                //we call the settings option
                Intent settingsActivityIntent=new Intent(getApplicationContext(), WelcomeScreenActivity.class);
                settingsActivityIntent.putExtra(WelcomeScreenActivity.EXTRA_SETTINGS_NAME,true);

                startActivity(settingsActivityIntent);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.mm_surf_sendFileArea:
                //we open the file explorer
                Intent intent=new Intent(this, FileBrowserAndQueueActivity.class);
                //Intent intent=new Intent(this,SenderPickDestinationActivity.class);
                startActivity(intent);
                break;
            case R.id.mm_surf_receiveArea:
                //we open the master picker
                Intent intentReceive=new Intent(this,ReceiverPickDestinationActivity.class);
                startActivity(intentReceive);
                break;
            default:
                break;
        }
    }
}