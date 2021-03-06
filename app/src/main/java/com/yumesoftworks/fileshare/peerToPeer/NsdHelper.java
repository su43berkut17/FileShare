package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;

public class NsdHelper {

    public String mServiceName = "FileShareNdsServiceForFileTransfer";
    public static final String SERVICE_TYPE = "_http._tcp";
    public static final String TAG = "NsdHelper";

    //network service discovery vars
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;

    //interface to send services added or deleted
    private ChangedServicesListener mServiceListener;

    public NsdHelper(Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        if (context instanceof ChangedServicesListener){
            mServiceListener=(ChangedServicesListener) context;
        }
    }

    public void initializeNsd() {
        /*if (mRegistrationListener==null) {
            initializeRegistrationListener();
        }*/
    }

    public void initializeDiscoveryListener() {
        Log.d(TAG,"initializeDiscoveryListener called");
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                //Log.d(TAG, "Service discovery started "+regType);
                mServiceListener.discoveryInitiated();
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                //refactor this so the service type is not making an error due to the serviceType method returning it ith an exta . at the end
                Log.d(TAG, "1-Service discovery success:" + service.getServiceName()+" "+service.getPort());

                //unknown service
                /*if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG,"Unkwown service type");
                }else*/
                /*if (service.getServiceName().equals(mServiceName)){
                    Log.d(TAG,"It is the same device "+service.getServiceName()+" compared to local "+mServiceName);
                }else */if(service.getServiceName().contains(mServiceName)||mServiceName.contains(service.getServiceName())){
                    //Log.d(TAG,"Comparing the received service name "+service.getServiceName()+" with local service name: "+mServiceName);

                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed " + errorCode+"service name: "+ serviceInfo.getServiceName()+" service type: "+serviceInfo.getServiceType());
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.d(TAG,"Service resolved "+serviceInfo.getPort()+"-"+serviceInfo.getHost());
                            //Log.d(TAG,"comparing ips, local: ");

                            //return found service to be added to the recycler view on the activity]
                            if (mServiceListener!=null) {
                                mServiceListener.addedService(serviceInfo);
                            }
                        }
                    });
                }else{
                    Log.d(TAG,"There was an error on service found");
                    Log.d(TAG,"Compared received service name "+service.getServiceName()+" with local service name: "+mServiceName);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost " + service);
                /*if (mService == service) {
                    mService = null;
                }*/

                //we delete service from the list
                Log.d(TAG, "Service unregistered, we send it to the activity: " + service.getServiceName());
                try{
                    mServiceListener.removedService(service);
                }catch (Exception e){
                    Log.d(TAG,"Cannot remove service since we unregistered the registration");
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery start failed: Error code:" + errorCode);
                mServiceListener.discoveryFailed();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: Error code:" + errorCode);
            }
        };
    }

    public void initializeRegistrationListener() {
        Log.d(TAG,"initializeRegistrationListener called");
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                //mServiceName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered: " + NsdServiceInfo.getServiceName());
                mServiceListener.serviceRegistered();
            }
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "Service registration failed: " + arg1);
                mServiceListener.serviceRegistrationError();
            }
            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "Service unregistered, we send it to the activity: " + arg0.getServiceName());
                try{
                    mServiceListener.removedService(arg0);
                }catch (Exception e){
                    Log.d(TAG,"Cannot remove service since we unregistered the registration");
                }
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

        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        //Log.d(TAG,"Registering service type: "+SERVICE_TYPE+"-- the service info has as: "+serviceInfo.getServiceType());
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

        //Log.d(TAG,"The register service ran, the service type inside the ndsmanager is "+mNsdManager);
    }

    public void discoverServices() {
        Log.d(TAG,"Call to discover services");
        // Cancel any existing discovery request
        stopDiscovery();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        //Log.d(TAG,"Cancelling former discovery");
        if (mDiscoveryListener != null) {
            Log.d(TAG,"mDiscoverListener is no null so we stop service and set it to null");
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (Exception e) {
                Log.d(TAG,"Couldnt Stop the service discovery"+e.getMessage());
            }finally {
            }
            mDiscoveryListener = null;
        }
    }

    public void cancelRegistration() {
        //Log.d(TAG,"Cancelling previous request");
        if (mRegistrationListener != null) {
            Log.d(TAG,"mRegistration listener is not null so we unregister and then st to null");
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            }catch(Exception e){
                Log.d(TAG,"Couldnt cancel the registratior");
            } finally {
            }
            mRegistrationListener = null;
        }
    }

    //interface
    public interface ChangedServicesListener{
        //sender
        void addedService(NsdServiceInfo serviceInfo);
        void removedService(NsdServiceInfo serviceInfo);
        void discoveryInitiated();
        void discoveryFailed();

        //receiver
        void serviceRegistered();
        void serviceRegistrationError();
    }
}