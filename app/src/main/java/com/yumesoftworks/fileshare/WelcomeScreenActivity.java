package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;
import com.yumesoftworks.fileshare.data.UserInfoEntry;
import com.yumesoftworks.fileshare.recyclerAdapters.AvatarAdapter;
import com.yumesoftworks.fileshare.utils.JsonAvatarParser;

import java.util.ArrayList;
import java.util.List;

public class WelcomeScreenActivity extends AppCompatActivity implements AvatarAdapter.ItemClickListener {
    private static final String TAG=WelcomeScreenActivity.class.getSimpleName();

    //this member variable will let us know if this activity is opened as settings or the first time
    private boolean mIsThisSettings;

    private RecyclerView rvAvatars;
    private AvatarAdapter mAvatarAdapter;
    private AvatarAndVersion mAvatarAndVersion;

    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        //initialize rv
        rvAvatars=findViewById(R.id.rv_aws_avatars);
        rvAvatars.setLayoutManager(new GridLayoutManager(this,4));

        //avatar rv adapter
        mAvatarAdapter=new AvatarAdapter(this,this);
        rvAvatars.setAdapter(mAvatarAdapter);

        //TODO: feed the adapter with the 8 standard icons
        /*mListAvatars=new ArrayList<>();
        for (int i=0;i<8;i++){

        }
        mListAvatars.add();*/
        JsonAvatarParser parser=new JsonAvatarParser(this);
        parser.loadData();

        mDb=AppDatabase.getInstance(getApplicationContext());
        setupViewModel();
    }

    //view model
    private void setupViewModel(){
        WelcomeScreenViewModel viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {
                //TODO: if there is data in the database we show it in the textviews and in the adapter if it is empty then we dont do anything

            }
        });
    }

    //listener of click in each avatar
    @Override
    public void onItemClickListener(int itemId) {
        //TODO: highlight the selected icon
    }
}
