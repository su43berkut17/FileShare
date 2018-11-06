package com.yumesoftworks.fileshare;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class ReceiverPickDestinationActivity extends AppCompatActivity {

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_pick_destination);

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);
    }
}
