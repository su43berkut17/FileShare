package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.UserInfoEntry;

import java.util.List;

public class WelcomeScreenActivity extends AppCompatActivity {
    private static final String TAG=WelcomeScreenActivity.class.getSimpleName();

    private RecyclerView rvAvatars;
    //TODO: adapter for the rv

    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        //initialize rv
        rvAvatars=findViewById(R.id.rv_aws_avatars);
        rvAvatars.setLayoutManager(new GridLayoutManager(this,4));

        //TODO: init adapter


        mDb=AppDatabase.getInstance(getApplicationContext());
        setupViewModel();
    }

    //view model
    private void setupViewModel(){
        WelcomeScreenViewModel viewModel=ViewModelProviders.of(this).get(WelcomeScreenViewModel.class);
        viewModel.getUserInfo().observe(this, new Observer<List<UserInfoEntry>>() {
            @Override
            public void onChanged(@Nullable List<UserInfoEntry> userInfoEntries) {
                //TODO: set the adapter to userInfo
            }
        });
    }
}
