package com.yumesoftworks.fileshare;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener{
    //buttons
    ConstraintLayout sendFilesButton;
    ConstraintLayout receiveFilesButton;

    private static final String TAG="MainMenuActivity";

    //view model
    private CombinedDataViewModel fileViewerViewModel;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;
    private AdView mAdView;

    //loading for 1st run
    private LinearLayout mLoadingScreen;
    private int mIsTransferInProgress=TransferProgressActivity.STATUS_TRANSFER_INACTIVE;

    //database
    private WelcomeScreenViewModel viewModel;

    //context
    private Context thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        //this activity
        thisActivity=this;

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_main_menu);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //loading screen
        mLoadingScreen=findViewById(R.id.wel_loading_layout);

        //we set the values of the constraint layouts
        sendFilesButton=(ConstraintLayout)findViewById(R.id.mm_surf_sendFileArea);
        receiveFilesButton=(ConstraintLayout)findViewById(R.id.mm_surf_receiveArea);

        //we set the click listeners on the buttons
        sendFilesButton.setOnClickListener(this);
        receiveFilesButton.setOnClickListener(this);

        //we empty the stored database
        fileViewerViewModel= ViewModelProviders.of(this).get(CombinedDataViewModel.class);
        fileViewerViewModel.deleteTable();

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mm_toolbar);
        setSupportActionBar(myToolbar);

        //detect if this is the 1st run
        setupViewModel();
    }

    //view model
    private void setupViewModel(){
        viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {

                if (userInfoEntries.isEmpty()){
                    //we open the setup
                    Intent settingsActivityIntent=new Intent(getApplicationContext(), WelcomeScreenActivity.class);
                    //clear backstack
                    settingsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(settingsActivityIntent);
                    finish();
                }else{
                    //we check if a transfer is in progress
                    mIsTransferInProgress=userInfoEntries.get(0).getIsTransferInProgress();
                    if (mIsTransferInProgress!=TransferProgressActivity.STATUS_TRANSFER_INACTIVE){
                        Log.e(TAG,"This is a relaunch from the main menu nactivity");
                        //we relaunch the transfer activity
                        Intent intent= new Intent(getApplicationContext(), com.yumesoftworks.fileshare.TransferProgressActivity.class);

                        //set the extra
                        Bundle extras=new Bundle();
                        extras.putInt(com.yumesoftworks.fileshare.TransferProgressActivity.EXTRA_TYPE_TRANSFER, com.yumesoftworks.fileshare.TransferProgressActivity.RELAUNCH_APP);
                        intent.putExtras(extras);

                        //clear backstack
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                        //basic transition to main menu
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation((Activity)thisActivity).toBundle();
                            startActivity(intent, bundle);
                        } else {
                            startActivity(intent);
                        }

                        //finish this activity
                        finish();
                    }else {
                        mLoadingScreen.setVisibility(View.GONE);
                    }
                }
            }
        });
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