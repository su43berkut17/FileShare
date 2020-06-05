package com.yumesoftworks.fileshare;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

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

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.ads.consent.*;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener{
    //buttons
    ConstraintLayout sendFilesButton;
    ConstraintLayout receiveFilesButton;

    private static final String TAG="MainMenuActivity";

    //view model
    private CombinedDataViewModel fileViewerViewModel;

    //admob
    private AdView mAdView;
    private ConsentForm form;

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

        //consent
        ConsentInformation consentInformation = ConsentInformation.getInstance(thisActivity);
        consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        String[] publisherIds = {"pub-0123456789012345"};
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                // User's consent status successfully updated.
                if (consentStatus==ConsentStatus.PERSONALIZED){
                    MobileAds.initialize(thisActivity,
                            "ca-app-pub-3940256099942544/6300978111");

                    mAdView = findViewById(R.id.ad_view_main_menu);
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mAdView.loadAd(adRequest);

                    //Crash logging
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

                    setupViewModel();
                }else if(consentStatus==ConsentStatus.NON_PERSONALIZED){
                    MobileAds.initialize(thisActivity,
                            "ca-app-pub-3940256099942544/6300978111");

                    mAdView = findViewById(R.id.ad_view_main_menu);

                    Bundle extras = new Bundle();
                    extras.putString("npa", "1");

                    AdRequest adRequest = new AdRequest.Builder()
                            .addNetworkExtrasBundle(AdMobAdapter.class,extras)
                            .build();
                    mAdView.loadAd(adRequest);

                    //Crash logging
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);

                    setupViewModel();
                }else{
                    //initialize consent dialog
                    URL privacyUrl = null;
                    try {
                        // TODO: Replace with your app's privacy policy URL.
                        privacyUrl = new URL("https://www.yumesoftworks.com/");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();

                        //Crash logging
                        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
                        setupViewModel();
                    }

                    form= new ConsentForm.Builder(thisActivity, privacyUrl)
                    .withListener(new ConsentFormListener() {
                        @Override
                        public void onConsentFormLoaded() {
                            // Consent form loaded successfully.
                            form.show();
                        }

                        @Override
                        public void onConsentFormOpened() {
                            // Consent form was displayed.
                        }

                        @Override
                        public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                            // Consent form was closed.
                            ConsentInformation.getInstance(thisActivity).setConsentStatus(consentStatus);

                            //check consent status for crash logging
                            if (consentStatus==ConsentStatus.PERSONALIZED){
                                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
                            }else{
                                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
                            }

                            //continue loading
                            setupViewModel();
                        }

                        @Override
                        public void onConsentFormError(String errorDescription) {
                            // Consent form error.
                            Log.e(TAG,"Coudln't show form "+errorDescription);

                            //Crash logging
                            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);

                            setupViewModel();
                        }
                    })
                    .withPersonalizedAdsOption()
                    .withNonPersonalizedAdsOption()
                    .withAdFreeOption()
                    .build();
                    form.load();
                }
            }

            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                // User's consent status failed to update.
                Log.e(TAG,"Cannot initiate ads "+errorDescription);

                //Crash logging
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);

                setupViewModel();
            }
        });

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
                    mLoadingScreen.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        //remove observers
        if (viewModel!=null && viewModel.getUserInfo().hasObservers()) {
            viewModel.getUserInfo().removeObservers(this);
        }

        super.onDestroy();
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