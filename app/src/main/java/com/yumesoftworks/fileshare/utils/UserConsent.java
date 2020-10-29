package com.yumesoftworks.fileshare.utils;

import android.content.Context;
import android.util.Log;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;
import com.yumesoftworks.fileshare.ConstantValues;

import java.net.MalformedURLException;
import java.net.URL;

public class UserConsent {
    private String TAG="UserConsent";

    //Form
    private ConsentForm form;
    private Context mContext;

    //General consent
    private ConsentInformation consentInformation;

    //interface to send services added or deleted
    private UserConsentInterface mServiceListener;
    private UserConsentISEEA mUSerConsentISEEA;

    public UserConsent(Context context) {
        mContext = context;

        consentInformation = ConsentInformation.getInstance(mContext);

        if (context instanceof UserConsentInterface) {
            mServiceListener = (UserConsentInterface) context;
        }
        if (context instanceof UserConsentISEEA){
            mUSerConsentISEEA=(UserConsentISEEA) context;
        }
    }

    public void checkConsent(){
        Log.d(TAG,"Checking the user consent");
        //consent
        //consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        String[] publisherIds = {ConstantValues.admob_pub_id};
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                if (!ConsentInformation.getInstance(mContext).isRequestLocationInEeaOrUnknown()) {
                    mServiceListener.initAd(true);
                    if (mUSerConsentISEEA!=null) {
                        mUSerConsentISEEA.isEEA(false);
                    }

                    Log.d(TAG,"Not in EEA");
                }else {
                    if (mUSerConsentISEEA!=null) {
                        mUSerConsentISEEA.isEEA(true);
                    }
                    // User's consent status successfully updated.
                    if (consentStatus == ConsentStatus.PERSONALIZED) {
                        Log.d(TAG,"Consent is true");
                        mServiceListener.initAd(true);
                    } else if (consentStatus == ConsentStatus.NON_PERSONALIZED) {
                        Log.d(TAG,"Consent is false");
                        mServiceListener.initAd(false);
                    } else {
                        generateForm();
                    }
                }
            }

            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                // User's consent status failed to update.
                Log.e(TAG,"Cannot initiate ads "+errorDescription);

                mServiceListener.initAd(false);
            }
        });
    }

    public void generateForm(){
        Log.d(TAG,"Generating the form");
        //initialize consent dialog
        URL privacyUrl = null;
        try {
            // TODO: Replace with your app's privacy policy URL.
            privacyUrl = new URL("https://www.yumesoftworks.com/fileshareapp/fileshare-privacy-policy/");
        } catch (MalformedURLException e) {
            e.printStackTrace();

            mServiceListener.initAd(false);
        }

        form = new ConsentForm.Builder(mContext, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        form.show();
                    }

                    @Override
                    public void onConsentFormOpened() {
                        // Consent form was displayed.
                    }

                    @Override
                    public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        // Consent form was closed.
                        ConsentInformation.getInstance(mContext).setConsentStatus(consentStatus);

                        //check consent status for crash logging
                        if (consentStatus == ConsentStatus.PERSONALIZED) {
                            mServiceListener.initAd(true);
                        } else {
                            mServiceListener.initAd(false);
                        }
                    }

                    @Override
                    public void onConsentFormError(String errorDescription) {
                        // Consent form error.
                        Log.e(TAG, "Couldn't show form " + errorDescription);

                        mServiceListener.initAd(false);
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .withAdFreeOption()
                .build();
        form.load();
    }

    public interface UserConsentInterface {
        void initAd(Boolean isTracking);
    }

    public interface UserConsentISEEA{
        void isEEA(Boolean isEEA);
    }
}