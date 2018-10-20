package com.yumesoftworks.fileshare;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SenderPickDestinationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_pick_destination);

        //create the connection
        createConnection();
    }

    private void createConnection(){

    }
}
