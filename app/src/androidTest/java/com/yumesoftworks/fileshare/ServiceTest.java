package com.yumesoftworks.fileshare;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ServiceTest {
    private ServiceFileShare mServiceSend;
    private ServiceFileShare mServiceReceive;

    @Rule
    public final ServiceTestRule serviceRuleSender = new ServiceTestRule();
    public final ServiceTestRule serviceRuleReceiver = new ServiceTestRule();

    //Should return false since transfer is not active
    @Test
    public void createService(){
        // Create the service Intent for the sender
        Intent serviceIntentSender =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ServiceFileShare.class);

        // Create the service Intent for the receiver
        Intent serviceIntentReceiver =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ServiceFileShare.class);

        //mock data for sender service
        Bundle bundleSend = new Bundle();

        //local ip and port
        bundleSend.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_SENDING);
        bundleSend.putString(TransferProgressActivity.LOCAL_IP, "10.0.2.15");
        bundleSend.putInt(TransferProgressActivity.LOCAL_PORT, 2245);
        bundleSend.putString(TransferProgressActivity.REMOTE_IP, "10.0.2.15");
        bundleSend.putInt(TransferProgressActivity.REMOTE_PORT, 2245);

        //mod data for receiver service
        Bundle bundleRec=new Bundle();
        bundleRec.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_RECEIVING);
        bundleRec.putInt(TransferProgressActivity.LOCAL_PORT, 2245);

        // Data can be passed to the service via the Intent.
        serviceIntentSender.putExtra(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.SERVICE_TYPE_SENDING);
        serviceIntentReceiver.putExtra(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.SERVICE_TYPE_RECEIVING);

        // Bind the service and grab a reference to the binder for the sender
        try {
            IBinder binderSend = serviceRuleSender.bindService(serviceIntentSender);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            mServiceSend =((ServiceFileShare.ServiceFileShareBinder)binderSend).getService();
            mServiceSend.onStartCommand(serviceIntentReceiver,0,113);
            assertThat(mServiceSend.methodIsTransferActive(),is(true));

        }catch (TimeoutException e){
            fail();
        }

        //Bind the service for the receiver
        try {
            IBinder binderRec = serviceRuleReceiver.bindService(serviceIntentReceiver);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            mServiceReceive =((ServiceFileShare.ServiceFileShareBinder)binderRec).getService();
            mServiceReceive.onStartCommand(serviceIntentReceiver,0,112);
            assertThat(mServiceReceive.methodIsTransferActive(),is(true));

        }catch (TimeoutException e){
            fail();
        }
    }

    @Test
    public void createSenderService(){
        // Create the service Intent for the sender
        Intent serviceIntentSender =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ServiceFileShare.class);


        //mock data for sender service
        Bundle bundleSend = new Bundle();

        //local ip and port
        bundleSend.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_SENDING);
        bundleSend.putString(TransferProgressActivity.LOCAL_IP, "10.0.2.15");
        bundleSend.putInt(TransferProgressActivity.LOCAL_PORT, 2245);
        bundleSend.putString(TransferProgressActivity.REMOTE_IP, "10.0.2.15");
        bundleSend.putInt(TransferProgressActivity.REMOTE_PORT, 2245);

        serviceIntentSender.putExtras(bundleSend);

        // Bind the service and grab a reference to the binder for the sender
        try {
            IBinder binderSend = serviceRuleSender.bindService(serviceIntentSender);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            mServiceSend =((ServiceFileShare.ServiceFileShareBinder)binderSend).getService();
            mServiceSend.onStartCommand(serviceIntentSender,0,113);
            assertThat(mServiceSend.methodIsTransferActive(),is(true));

        }catch (TimeoutException e){
            fail();
        }
    }

    @Test
    public void createReceiverService(){
        // Create the service Intent for the receiver
        Intent serviceIntentReceiver =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ServiceFileShare.class);

        //mod data for receiver service
        Bundle bundleRec=new Bundle();
        bundleRec.putInt(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.FILES_RECEIVING);
        bundleRec.putInt(TransferProgressActivity.LOCAL_PORT, 2245);

        // Data can be passed to the service via the Intent.
        serviceIntentReceiver.putExtras(bundleRec);

        //Bind the service for the receiver
        try {
            IBinder binderRec = serviceRuleReceiver.bindService(serviceIntentReceiver);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            mServiceReceive =((ServiceFileShare.ServiceFileShareBinder)binderRec).getService();
            mServiceReceive.onStartCommand(serviceIntentReceiver,0,112);
            assertThat(mServiceReceive.methodIsTransferActive(),is(true));

        }catch (TimeoutException e){
            fail();
        }
    }
}
