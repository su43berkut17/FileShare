package com.yumesoftworks.fileshare.data;

import java.net.InetAddress;

public class UserSendEntry {
    private String username;
    private int avatar;
    private String infoToSend;
    private InetAddress ipAddress;
    private int port;

    public UserSendEntry(String username, int avatar, String infoToSend, InetAddress ipAddress, int port){
        this.username=username;
        this.avatar=avatar;
        this.infoToSend=infoToSend;
        this.ipAddress=ipAddress;
        this.port=port;
    }

    //getters and setters
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public int getAvatar() {
        return avatar;
    }
    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public String getInfoToSend() {
        return infoToSend;
    }
    public void setInfoToSend(String infoToSend) {
        this.infoToSend = infoToSend;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
}
