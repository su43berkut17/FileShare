package com.yumesoftworks.fileshare;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;

import android.content.UriPermission;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

import com.yumesoftworks.fileshare.utils.UserConsent;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener, UserConsent.UserConsentInterface {
    //buttons
    ConstraintLayout sendFilesButton;
    ConstraintLayout receiveFilesButton;

    private static final String TAG="MainMenuActivity";

    //view model
    private CombinedDataViewModel fileViewerViewModel;

    //admob
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

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mm_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //empty the file list
        fileViewerViewModel.deleteTable();

        //check the user consent
        UserConsent userConsent=new UserConsent(thisActivity);
        userConsent.checkConsent();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //remove observers
        if (viewModel!=null && viewModel.getUserInfo().hasObservers()) {
            viewModel.getUserInfo().removeObservers(this);
        }
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
                        //we reset the values
                        UserInfoEntry resetUserInfoEntry=userInfoEntries.get(0);
                        resetUserInfoEntry.setIsTransferInProgress(TransferProgressActivity.STATUS_TRANSFER_INACTIVE);
                        resetUserInfoEntry.setTransferTypeSendOrReceive(TransferProgressActivity.SERVICE_TYPE_INACTIVE);
                        viewModel.resetFlags(resetUserInfoEntry);

                        //return the widget to its normal state
                        try {
                            updateWidgetService.startActionUpdateWidget(thisActivity, TransferProgressWidget.STATE_NORMAL, "", 0, 0,100);
                        }catch (Exception e){
                            Log.e(TAG,"Couldn't set widget as normal");
                        }
                    }

                    //release saf persistent permissions for files
                    if (Build.VERSION.SDK_INT>=ConstantValues.SAF_SDK){
                        removePersistentFilePermissions();
                    }

                    mLoadingScreen.setVisibility(View.GONE);
                }
            }
        });
    }

    //remove permissions
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void removePersistentFilePermissions(){
        //release saf persistent permissions for files
        List<UriPermission> permissionList = thisActivity.getContentResolver().getPersistedUriPermissions();
        Log.d(TAG,"Persisted permissions: "+permissionList.size());

        for (UriPermission permission:permissionList
        ) {
            Log.d(TAG,"Persisted uri:"+permission.getUri().toString());
            thisActivity.getContentResolver().releasePersistableUriPermission(permission.getUri(),Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
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

    @Override
    public void initAd(Boolean isTracking) {
        MobileAds.initialize(thisActivity,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_main_menu);
        AdRequest adRequest;

        if (isTracking){
            adRequest = new AdRequest.Builder().build();
        }else{
            Bundle extras = new Bundle();
            extras.putString("npa", "1");

            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class,extras)
                    .build();
        }

        mAdView.loadAd(adRequest);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isTracking);
        setupViewModel();
    }
}