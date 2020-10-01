package com.yumesoftworks.fileshare;

import android.app.ActivityOptions;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.multidex.BuildConfig;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.data.AvatarDefaultImages;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.AvatarAdapter;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;
import com.yumesoftworks.fileshare.utils.UserConsent;

import java.util.List;

public class WelcomeScreenActivity extends AppCompatActivity implements AvatarAdapter.ItemClickListener,
        View.OnClickListener,
        UserConsent.UserConsentInterface,
        UserConsent.UserConsentISEEA {

    private static final String TAG=WelcomeScreenActivity.class.getSimpleName();
    private static final String NAME_ROTATION_AVATAR_STATE="savedAvatarId";
    public static final String EXTRA_SETTINGS_NAME="isThisSettings";

    //this member variable will let us know if this activity is opened as settings or the first time
    private boolean mIsThisSettings;
    private int mSelectedAvatar=-1;
    private int mFilesTransferred=0;
    private int mVersion=-1;
    private boolean mIsAndroid11SafDialogShown=false;

    //recycler view
    private RecyclerView rvAvatars;
    private AvatarAdapter mAvatarAdapter;

    //widgets
    private TextView tvUsername;
    private Button buttonGo;
    private Button buttonCancel;
    private Button buttonHelp;
    private Button buttonUnlockAds;
    private LinearLayout lineaLayoutGDRP;
    private Switch switchGDRP;
    private Boolean mSwitchAllowed=false;

    private Context mContext;

    //database
    private WelcomeScreenViewModel viewModel;
    private int mIsTransferInProgress=TransferProgressActivity.STATUS_TRANSFER_INACTIVE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        mContext=this;

        //initialize linear layout and switch
        lineaLayoutGDRP=findViewById(R.id.ll_aws_gdrp);
        switchGDRP=findViewById(R.id.swi_aws_gdrp);

        if (BuildConfig.FLAVOR.equals("paid")){
            lineaLayoutGDRP.setVisibility(View.GONE);
        }else {
            //check the user consent
            UserConsent userConsent = new UserConsent(this);
            userConsent.checkConsent();

            //listener
            switchGDRP.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mSwitchAllowed) {
                    userConsent.generateForm();
                    mSwitchAllowed = false;
                }
            });
        }

        //rotation values
        if (savedInstanceState==null){
            //we load the avatar value from the intent or default value if the intent does not exist
            //is this settings
            Intent intent = getIntent();
            mIsThisSettings = intent.getBooleanExtra(EXTRA_SETTINGS_NAME, false);
        }else{
            //we load it from the previous state
            mSelectedAvatar=savedInstanceState.getInt(NAME_ROTATION_AVATAR_STATE);
            mIsThisSettings = savedInstanceState.getBoolean(EXTRA_SETTINGS_NAME);
        }

        //initialize rv
        rvAvatars=findViewById(R.id.rv_aws_avatars);
        rvAvatars.setLayoutManager(new GridLayoutManager(this,4));

        //avatar rv adapter
        mAvatarAdapter=new AvatarAdapter(this,this);
        rvAvatars.setAdapter(mAvatarAdapter);

        //add the 8 standard avatars in the adapter
        mAvatarAdapter.setAvatar(AvatarDefaultImages.getDefaultImages());

        //set the listener in the button
        buttonGo= findViewById(R.id.button_go);
        buttonCancel=findViewById(R.id.button_cancel);
        buttonUnlockAds=findViewById(R.id.button_unlock_ads);
        buttonHelp=findViewById(R.id.button_help);
        tvUsername=findViewById(R.id.tv_aws_input_username);

        tvUsername.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId==EditorInfo.IME_ACTION_UNSPECIFIED) {
                saveChanges();
                handled = true;
            }
            return handled;
        });

        buttonGo.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        buttonHelp.setOnClickListener(this);

        if (BuildConfig.FLAVOR.equals("free")){
            buttonUnlockAds.setOnClickListener(this);
        }else{
            buttonUnlockAds.setVisibility(View.GONE);
        }

        setupViewModel();

        //toolbar
        Toolbar myToolbar = findViewById(R.id.aws_toolbar);
        setSupportActionBar(myToolbar);

        //navigation bar settings
        if (getSupportActionBar()!=null) {
            if (!mIsThisSettings) {
                getSupportActionBar().hide();
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //we save the data to restore on rotation
        outState.putInt(NAME_ROTATION_AVATAR_STATE,mSelectedAvatar);
        outState.putBoolean(EXTRA_SETTINGS_NAME,mIsThisSettings);
    }

    //view model
    private void setupViewModel(){
        viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.getUserInfo().observe(this, userInfoEntries -> {
            //for 1st run
            if (userInfoEntries.isEmpty()){
                //we hide the cancel button and the loading screen
                buttonCancel.setVisibility(View.GONE);
            }else{
                //for settings
                if (mIsThisSettings){
                    //we change the go button text to save changes
                    buttonGo.setText(R.string.aws_button_save);

                    //we set the loaded data to the ui
                    mSelectedAvatar=userInfoEntries.get(0).getPickedAvatar();
                    mVersion=userInfoEntries.get(0).getAssetVersion();
                    mFilesTransferred=userInfoEntries.get(0).getNumberFilesTransferred();
                    tvUsername.setText(userInfoEntries.get(0).getUsername());
                    mIsTransferInProgress=userInfoEntries.get(0).getIsTransferInProgress();
                    mIsAndroid11SafDialogShown=userInfoEntries.get(0).getAndroid11SafWarning();

                    //set the selected avatar
                    mAvatarAdapter.setSelectedAvatar(mSelectedAvatar);
                }
            }
        });
    }

    private void goMainActivity(){
        Intent mainMenuActivity=new Intent(getApplicationContext(), com.yumesoftworks.fileshare.MainMenuActivity.class);

        //delete the backstack
        mainMenuActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        //basic transition to main menu
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(mainMenuActivity, bundle);
            finish();
        } else {
            startActivity(mainMenuActivity);
            finish();
        }
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.button_go:
               saveChanges();
                break;

            case R.id.button_cancel:
                //return to former activity
                super.onBackPressed();
                break;
            case R.id.button_unlock_ads:
                openUnlockAds();
                break;

            case R.id.button_help:
                openHelp();
                break;
        }
    }

    //save changes function
    private void saveChanges(){
        //save changes and either return to former activity or run new activity
        //we verify if there is data to save
        String username=tvUsername.getText().toString();
        if ((username.isEmpty()||(mSelectedAvatar==-1))){
            //we show the dialog
            MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this,R.style.MyDialog);

            alertDialogBuilder.setCancelable(false);
            if (mSelectedAvatar==-1) {
                alertDialogBuilder.setMessage(R.string.aws_dialog_avatar);
            }else{
                alertDialogBuilder.setMessage(R.string.aws_dialog_username);
            }
            alertDialogBuilder.setTitle(R.string.aws_dialog_title);
            alertDialogBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

            alertDialogBuilder.show();

        }else{
            //we save the data and open the activity
            UserInfoEntry dataToSave=new UserInfoEntry(tvUsername.getText().toString(),mSelectedAvatar,mFilesTransferred,mVersion,mIsTransferInProgress,TransferProgressActivity.SERVICE_TYPE_INACTIVE,mIsAndroid11SafDialogShown);
            viewModel.saveData(dataToSave);

            //check if it is setup or settings
            if (mIsThisSettings){
                super.onBackPressed();
            }else{
                goMainActivity();
            }
        }
    }

    //open unlock ads
    private void openUnlockAds(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=com.yumesoftworks.fileshare.paid"));
        intent.setPackage("com.android.vending");
        startActivity(intent);
    }

    //open help
    private void openHelp(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://www.yumesoftworks.com/"));
        startActivity(intent);
    }

    //listener of click in each avatar
    @Override
    public void onItemClickListener(int itemId) {
        mSelectedAvatar=itemId;
        mAvatarAdapter.setSelectedAvatar(itemId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void initAd(Boolean isTracking) {
        switchGDRP.setChecked(isTracking);
        mSwitchAllowed=true;
    }

    @Override
    public void isEEA(Boolean isEEA) {
        if (!isEEA || !mIsThisSettings) {
            lineaLayoutGDRP.setVisibility(View.GONE);
        }
    }
}