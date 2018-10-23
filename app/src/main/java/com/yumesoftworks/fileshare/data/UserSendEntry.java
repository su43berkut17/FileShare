package com.yumesoftworks.fileshare.data;

import android.graphics.Bitmap;

public class UserSendEntry {
    private String username;
    private Bitmap avatar;
    private String infoToSend;

    public UserSendEntry(String username, Bitmap avatar, String infoToSend){
        this.username=username;
        this.avatar=avatar;
        this.infoToSend=infoToSend;
    }

    //getters and setters
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public Bitmap getAvatar() {
        return avatar;
    }
    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }

    public String getInfoToSend() {
        return infoToSend;
    }
    public void setInfoToSend(String infoToSend) {
        this.infoToSend = infoToSend;
    }
}
