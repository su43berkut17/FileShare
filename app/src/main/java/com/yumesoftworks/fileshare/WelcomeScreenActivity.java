package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.data.AvatarDefaultImages;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.AvatarAdapter;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import java.util.ArrayList;
import java.util.List;

public class WelcomeScreenActivity extends AppCompatActivity implements AvatarAdapter.ItemClickListener,
        JsonAvatarParser.OnLoadedAvatars,
        View.OnClickListener{
    private static final String TAG=WelcomeScreenActivity.class.getSimpleName();

    //this member variable will let us know if this activity is opened as settings or the first time
    private boolean mIsThisSettings;
    private int mSelectedAvatar=-1;

    //recycler view
    private RecyclerView rvAvatars;
    private AvatarAdapter mAvatarAdapter;

    //widgets
    private TextView tvUsername;
    private Button buttonGo;
    private Button buttonCancel;

    //database
    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        //is this settings
        Intent intent = getIntent();
        mIsThisSettings = intent.getBooleanExtra("isThisSettings",false);

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

        buttonGo.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);

        mDb=AppDatabase.getInstance(getApplicationContext());
        setupViewModel();

        mIsThisSettings=true;
        //navigation bar settings
        if (mIsThisSettings==false) {
            getSupportActionBar().hide();
        }
    }

    //view model
    private void setupViewModel(){
        WelcomeScreenViewModel viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {
                //TODO: if there is data in the database we show it in the textviews and in the adapter if it is empty then we dont do anything
                if (userInfoEntries.isEmpty()){
                    //we hide the cancel button
                    buttonCancel.setVisibility(View.GONE);
                }else{
                    //we go to the next activity if it is not settings
                    if (mIsThisSettings){
                        //this is settings so we feed the saved information to the UI
                        mAvatarAdapter.setSelectedAvatar(userInfoEntries.get(0).getId());

                        //we change the cancel button text to save changes
                        buttonGo.setText(R.string.aws_button_save);
                    }else{
                        //we open the main activity
                        goMainActivity();
                    }
                }
            }
        });
    }

    private void goMainActivity(){
        Intent mainMenuActivity=new Intent(getApplicationContext(), MainMenuActivity.class);
        startActivity(mainMenuActivity);
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.button_go:
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
                    alertDialogBuilder.setPositiveButton("tok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alertDialogBuilder.show();

                }else{
                    //we save the data and open the activity
                    //find a better way to save the data from the avatars
                    //UserInfoEntry dataToSave=new UserInfoEntry(tvUsername.getText().toString(),"",0,1);

                    //int id,String username, String pickedAvatar, int numberFilesTransferred,int assetVersion
                    //WelcomeScreenViewModel viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
                    //viewModel.saveData(dataToSave);

                    //go to main activity
                    goMainActivity();
                }
                break;

            case R.id.button_cancel:
                //return to activity

                break;
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
        if (retAvatarAndVersion!=null){
            List<AvatarStaticEntry> receivedAvatars=AvatarDefaultImages.getDefaultImages();
            receivedAvatars.addAll(retAvatarAndVersion.getAvatarList());

            mAvatarAdapter.setAvatar(receivedAvatars);
        }
    }
}