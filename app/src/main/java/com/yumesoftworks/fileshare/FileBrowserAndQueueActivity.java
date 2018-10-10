package com.yumesoftworks.fileshare;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

//this activity will change depending if it is a tablet view
public class FileBrowserAndQueueActivity extends AppCompatActivity {
    //2 panel
    private boolean mTwoPanel;

    //fragment p[arts
    private FileViewer fragmentFileViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser_and_queue);

        //we check if it is 1 or 2 panels
        mTwoPanel=false;

        //we check for the permissions
        askForFilePermission();
    }

    private void initializeVariables(){
        //fragment stuff
        FragmentManager fragmentManager=getSupportFragmentManager();

        fragmentFileViewer=new FileViewer();

        //we load the files
        loadFragments(fragmentManager);
    }

    private void loadFragments(FragmentManager fragmentManager){
        fragmentManager.beginTransaction()
                .add(R.id.frag_afv_main, fragmentFileViewer)
                .commit();
    }

    private void askForFilePermission(){
        //we ask for permission before continuing
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //we create the fragments
                initializeVariables();
            } else {
                //we ask for permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            //we create the fragments
            initializeVariables();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //we create the fragment
            initializeVariables();
        }else{
            //go back to main activity
            Intent intent=new Intent(this,MainMenuActivity.class);
            startActivity(intent);
        }
    }
}
