package com.yumesoftworks.fileshare;

import android.app.Activity;
import android.app.ActivityOptions;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.data.AvatarDefaultImages;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.AvatarAdapter;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import java.util.List;

public class WelcomeScreenActivity extends AppCompatActivity implements AvatarAdapter.ItemClickListener,
        JsonAvatarParser.OnLoadedAvatars,
        View.OnClickListener{

    private static final String TAG=WelcomeScreenActivity.class.getSimpleName();
    private static final String NAME_ROTATION_AVATAR_STATE="savedAvatarId";
    public static final String EXTRA_SETTINGS_NAME="isThisSettings";

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;

    //this member variable will let us know if this activity is opened as settings or the first time
    private boolean mIsThisSettings;
    private int mSelectedAvatar=-1;
    private int mFilesTransferred=0;
    private int mVersion=-1;

    //recycler view
    private RecyclerView rvAvatars;
    private AvatarAdapter mAvatarAdapter;

    //widgets
    private TextView tvUsername;
    private Button buttonGo;
    private Button buttonCancel;

    //database
    private WelcomeScreenViewModel viewModel;
    private int mIsTransferInProgress=TransferProgressActivity.STATUS_TRANSFER_INACTIVE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

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

        //load the remote avatars, we will use an interface once the avatars have loaded
        JsonAvatarParser parser=new JsonAvatarParser(this);
        parser.loadData();

        //set the listener in the button
        buttonGo=(Button)findViewById(R.id.button_go);
        buttonCancel=(Button)findViewById(R.id.button_cancel);
        tvUsername=(TextView)findViewById(R.id.tv_aws_input_username);

        tvUsername.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId==EditorInfo.IME_ACTION_UNSPECIFIED) {
                    saveChanges();
                    handled = true;
                }
                return handled;
            }
        });

        buttonGo.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);

        setupViewModel();

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.aws_toolbar);
        setSupportActionBar(myToolbar);

        //navigation bar settings
        if (mIsThisSettings==false) {
            getSupportActionBar().hide();
        }else{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //we save the data to restore on rotation
        outState.putInt(NAME_ROTATION_AVATAR_STATE,mSelectedAvatar);
        outState.putBoolean(EXTRA_SETTINGS_NAME,mIsThisSettings);
    }

    //view model
    private void setupViewModel(){
        viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {
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
                    }
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
        }
    }

    //save changes function
    private void saveChanges(){
        //save changes and either return to former activity or run new activity
        //we verify if there is data to save
        String username=tvUsername.getText().toString();
        if ((username.isEmpty()||(mSelectedAvatar==-1))){
            //we show the dialog
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(false);
            if (mSelectedAvatar==-1) {
                alertDialogBuilder.setMessage(R.string.aws_dialog_avatar);
            }else{
                alertDialogBuilder.setMessage(R.string.aws_dialog_username);
            }
            alertDialogBuilder.setTitle(R.string.aws_dialog_title);
            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            alertDialogBuilder.show();

        }else{
            //we save the data and open the activity
            UserInfoEntry dataToSave=new UserInfoEntry(tvUsername.getText().toString(),mSelectedAvatar,mFilesTransferred,mVersion,mIsTransferInProgress);
            viewModel.saveData(dataToSave);

            //check if it is setup or settings
            if (mIsThisSettings){
                super.onBackPressed();
            }else{
                goMainActivity();
            }
        }
    }

    //listener of click in each avatar
    @Override
    public void onItemClickListener(int itemId) {
        mSelectedAvatar=itemId;
        mAvatarAdapter.setSelectedAvatar(itemId);
    }

    @Override
    public void LoadedRemoteAvatars(AvatarAndVersion retAvatarAndVersion) {
        //if it is not null we load the new views, otherwise we don't do anything
        if (retAvatarAndVersion!=null) {
            //we store the version
            mVersion=retAvatarAndVersion.getVersion();

            List<AvatarStaticEntry> receivedAvatars = AvatarDefaultImages.getDefaultImages();
            receivedAvatars.addAll(retAvatarAndVersion.getAvatarList());

            mAvatarAdapter.setAvatar(receivedAvatars);
        }

        //set the selected avatar if it exists
        if (mSelectedAvatar!=-1){
            mAvatarAdapter.setSelectedAvatar(mSelectedAvatar);
        }
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
}