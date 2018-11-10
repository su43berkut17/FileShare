package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;

public class NsdHelper {

    public String mServiceName = "FileShareNdsServiceForFileTransfer";
    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String TAG = "NsdHelper";

    Context mContext;

    //network service discovery vars
    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    NsdServiceInfo mService;
    int mPort;

    Boolean isServer;

    public NsdHelper(Context context, Boolean recIsServer) {
        mContext = context;
        isServer=recIsServer;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeResolveListener();
        //mNsdManager.init(mContext.getMainLooper(), this);
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started "+regType);
            }
            @Override

            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "1-Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    //unknown service
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains(mServiceName)){
                    Log.d(TAG,"2-resolving service");
                    Log.d(TAG,"3-service data "+service.getHost()+"-port:"+service.getPort()+"-"+service.getServiceName()+service.getServiceType());
                    //mNsdManager.resolveService(service, mResolveListener);
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed " + errorCode);
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                            if (serviceInfo.getServiceName().equals(mServiceName)) {
                                Log.d(TAG, "Same IP.");
                                return;
                            }
                            Log.d(TAG,"Service resolved "+serviceInfo.getPort()+"-"+serviceInfo.getHost());
                            //mService = serviceInfo;
                        }
                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery start failed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: Error code:" + errorCode);
            }
        };
    }
    public void initializeResolveListener() {
        Log.d(TAG,"initializing nsd resolve listener");
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
            }
        };
    }

    public void initializeRegistrationListener() {
        Log.d(TAG,"initializeRegistrationListener called");
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered: " + mServiceName);
            }
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "Service registration failed: " + arg1);
            }
            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "Service unregistered: " + arg0.getServiceName());
            }
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Service unregistration failed: " + errorCode);
            }
        };
    }
    public void registerService(int port) {
        Log.d(TAG,"Registering service with port "+port);
        // Cancel any previous registration request
        //cancelPreviousRegRequest();

        initializeRegistrationListener();

        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void discoverServices() {
        Log.d(TAG,"Call to discover services");
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        Log.d(TAG,"Cancelling discovery");
        if (mDiscoveryListener != null) {
            Log.d(TAG,"mDiscoverListener is no null so we stop service and set it to null");
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    public void cancelPreviousRegRequest() {
        Log.d(TAG,"Cancelling previous request");
        if (mRegistrationListener != null) {
            Log.d(TAG,"mRegistration listener is not null so we unregister and then st to null");
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }
}