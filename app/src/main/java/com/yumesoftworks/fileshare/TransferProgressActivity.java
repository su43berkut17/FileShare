package com.yumesoftworks.fileshare;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;

public class TransferProgressActivity extends AppCompatActivity implements FileTransferProgress.OnFragmentInteractionListener,
FileTransferSent.OnFragmentInteractionListener{

    private static final String TAG="TransferProgressAct";

    //fragment parts
    private FileTransferProgress fragmentFileTransferProgress;
    private FileTransferSent fragmentFileTransferSent;
    private FragmentManager fragmentManager;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;

    //viewmodel
    private FileTransferViewModel fileTransferViewModel;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //ads
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544/6300978111");

        mAdView = findViewById(R.id.ad_view_transfer_progress);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //initialize fragments
        initializeFragments();
    }

    private void initializeFragments(){
        //manager
        fragmentManager=getSupportFragmentManager();

        //fragments
        fragmentFileTransferProgress=new FileTransferProgress();
        fragmentFileTransferSent=new FileTransferSent();

        //transaction
        fragmentManager.beginTransaction()
                .add(R.id.frag_atp_transfer_progress,fragmentFileTransferProgress)
                .add(R.id.frag_atp_transfer_sent,fragmentFileTransferSent)
                .commit();

        //we get the file model to populate the stuff
        fileTransferViewModel=ViewModelProviders.of(this).get(FileTransferViewModel.class);
        fileTransferViewModel.getFileListInfo().observe(this,fileTransferViewModelObserver);
    }

    //observer
    final Observer<List<FileListEntry>> fileTransferViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we create a list for the not transferred and one for the transferred
            fragmentFileTransferProgress.updateRV(fileListEntries);
            fragmentFileTransferSent.updateRV(fileListEntries);
        }
    };

    @Override
    public void onFragmentInteractionSent(Uri uri) {

    }

    @Override
    public void onFragmentInteractionProgress(Uri uri){

    }
}
