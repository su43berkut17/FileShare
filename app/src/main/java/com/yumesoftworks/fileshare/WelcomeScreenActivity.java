package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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

public class WelcomeScreenActivity extends AppCompatActivity implements AvatarAdapter.ItemClickListener, JsonAvatarParser.OnLoadedAvatars {
    private static final String TAG=WelcomeScreenActivity.class.getSimpleName();

    //this member variable will let us know if this activity is opened as settings or the first time
    private boolean mIsThisSettings;
    private int mSelectedAvatar;

    //recycler view
    private RecyclerView rvAvatars;
    private AvatarAdapter mAvatarAdapter;

    //widgets
    private TextView tvUsername;
    private Button buttonGo;

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

        mDb=AppDatabase.getInstance(getApplicationContext());
        setupViewModel();

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
                    //we stay
                }else{
                    //we go to the next activity if it is not settings
                    if (mIsThisSettings){
                        //this is settings so we feed the saved information to the UI
                        mAvatarAdapter.setSelectedAvatar(userInfoEntries.get(0).getId());
                    }else{
                        //we open the main activity
                        Intent mainMenuActivity=new Intent(getApplicationContext(), MainMenuActivity.class);
                        startActivity(mainMenuActivity);
                    }
                }
            }
        });
    }

    //on save preferences button
    public void onSave(){
        //find a better way to save the data from the avatars
        UserInfoEntry dataToSave=new UserInfoEntry(tvUsername.getText().toString(),"",0,1):

        //int id,String username, String pickedAvatar, int numberFilesTransferred,int assetVersion
        WelcomeScreenViewModel viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.saveData(dataToSave);
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
