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
    private int numberFilesTransferred;

    @Ignore
    public UserInfoEntry(String username, String pickedAvatar, int numberFilesTransferred){
        this.username=username;
        this.pickedAvatar=pickedAvatar;
        this.numberFilesTransferred=numberFilesTransferred;
    }

    public UserInfoEntry(int id,String username, String pickedAvatar, int numberFilesTransferred){
        this.id=id;
        this.username=username;
        this.pickedAvatar=pickedAvatar;
        this.numberFilesTransferred=numberFilesTransferred;
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

    public int getNumberFilesTransferred() {
        return numberFilesTransferred;
    }

    public void setNumberFilesTransferred(int numberFilesTransferred) {
        this.numberFilesTransferred = numberFilesTransferred;
    }
}