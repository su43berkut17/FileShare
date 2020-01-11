package com.yumesoftworks.fileshare;

import android.content.Intent;
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

        // Data can be passed to the service via the Intent.
        serviceIntentSender.putExtra(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.SERVICE_TYPE_SENDING);
        serviceIntentReceiver.putExtra(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.SERVICE_TYPE_RECEIVING);

        // Bind the service and grab a reference to the binder for the sender
        try {
            IBinder binder = serviceRuleSender.bindService(serviceIntentSender);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            mServiceSend =((ServiceFileShare.ServiceFileShareBinder)binder).getService();
            assertThat(mServiceSend.methodIsTransferActive(),is(false));

        }catch (TimeoutException e){
            fail();
        }

        //Bind the service for the receiver
        try {
            IBinder binder = serviceRuleSender.bindService(serviceIntentReceiver);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            mServiceReceive =((ServiceFileShare.ServiceFileShareBinder)binder).getService();
            assertThat(mServiceReceive.methodIsTransferActive(),is(false));

        }catch (TimeoutException e){
            fail();
        }

        // Verify that the service is working correctly.
        //serviceIntentSender(service.methodIsTransferActive()).asser
    }
}
