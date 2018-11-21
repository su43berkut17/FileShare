package com.yumesoftworks.fileshare.data;

import com.yumesoftworks.fileshare.peerToPeer.SenderPickSocket;

public class SocketListEntry {
    private String serviceName;
    private String ipAddress;
    private SenderPickSocket senderSocket;

    public SocketListEntry(String serviceName, String ipAddress, SenderPickSocket senderSocket){
        this.serviceName=serviceName;
        this.ipAddress=ipAddress;
        this.senderSocket=senderSocket;
    }

    public String getServiceName() {
        return serviceName;
    }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public SenderPickSocket getSenderSocket() {
        return senderSocket;
    }
    public void setSenderSocket(SenderPickSocket senderSocket) {
        this.senderSocket = senderSocket;
    }


}
