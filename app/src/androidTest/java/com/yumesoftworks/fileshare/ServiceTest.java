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
    private ServiceFileShare mService1;
    private ServiceFileShare mService2;

    @Rule
    public final ServiceTestRule serviceRuleSender = new ServiceTestRule();


    @Test
    public void createService(){
        mService1=new ServiceFileShare();
        mService2=new ServiceFileShare();

        // Create the service Intent.
        Intent serviceIntentSender =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ServiceFileShare.class);

        // Data can be passed to the service via the Intent.
        serviceIntentSender.putExtra(TransferProgressActivity.EXTRA_TYPE_TRANSFER, TransferProgressActivity.SERVICE_TYPE_SENDING);

        // Bind the service and grab a reference to the binder.
        try {
            IBinder binder = serviceRuleSender.bindService(serviceIntentSender);

            // Get the reference to the service, or you can call
            // public methods on the binder directly.
            ServiceFileShare service =((ServiceFileShare.ServiceFileShareBinder)binder).getService();
            assertThat(service.methodIsTransferActive(),is(false));

        }catch (TimeoutException e){
            fail();
        }



        // Verify that the service is working correctly.
        //serviceIntentSender(service.methodIsTransferActive()).asser
    }
}
