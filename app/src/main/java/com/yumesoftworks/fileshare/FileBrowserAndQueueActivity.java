package com.yumesoftworks.fileshare;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

//this activity will change depending if it is a tablet view
public class FileBrowserAndQueueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser_and_queue);
    }
}
