package com.yumesoftworks.fileshare.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity (tableName = "userInfo")
public class UserInfoEntry {

    @PrimaryKey (autoGenerate = true)
    private int id;
    private String username;
    private String pickedAvatar;

    @Ignore
    public UserInfoEntry(String username, String pickedAvatar){
        this.username=username;
        this.pickedAvatar=pickedAvatar;
    }

    public UserInfoEntry(int id,String username, String pickedAvatar){
        this.id=id;
        this.username=username;
        this.pickedAvatar=pickedAvatar;
    }

    //getters and setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPickedAvatar() {
        return pickedAvatar;
    }

    public void setPickedAvatar(String pickedAvatar) {
        this.pickedAvatar = pickedAvatar;
    }
}