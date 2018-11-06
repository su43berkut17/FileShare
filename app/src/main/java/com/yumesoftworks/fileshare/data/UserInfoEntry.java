package com.yumesoftworks.fileshare.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity (tableName = "userInfo")
public class UserInfoEntry {

    public static int TYPE_LOCAL=1001;
    public static int TYPE_REMOTE=1002;

    @PrimaryKey (autoGenerate = true)
    private int id;
    private String username;
    private int pickedAvatar;
    private int numberFilesTransferred;
    private int assetVersion;
    private int isTransferInProgress;

    @Ignore
    public UserInfoEntry(String username, int pickedAvatar, int numberFilesTransferred, int assetVersion, int isTransferInProgress){
        this.username=username;
        this.pickedAvatar=pickedAvatar;
        this.numberFilesTransferred=numberFilesTransferred;
        this.assetVersion=assetVersion;
        this.isTransferInProgress=isTransferInProgress;
    }

    public UserInfoEntry(int id, String username, int pickedAvatar, int numberFilesTransferred, int assetVersion, int isTransferInProgress){
        this.id=id;
        this.username=username;
        this.pickedAvatar=pickedAvatar;
        this.numberFilesTransferred=numberFilesTransferred;
        this.assetVersion=assetVersion;
        this.isTransferInProgress=isTransferInProgress;
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

    public int getPickedAvatar() {
        return pickedAvatar;
    }
    public void setPickedAvatar(int pickedAvatar) {
        this.pickedAvatar = pickedAvatar;
    }

    public int getNumberFilesTransferred() {
        return numberFilesTransferred;
    }
    public void setNumberFilesTransferred(int numberFilesTransferred) {
        this.numberFilesTransferred = numberFilesTransferred;
    }

    public int getAssetVersion() {
        return assetVersion;
    }
    public void setAssetVersion(int assetVersion) {
        this.assetVersion = assetVersion;
    }

    public int getIsTransferInProgress() {
        return isTransferInProgress;
    }
    public void setIsTransferInProgress(int isTransferInProgress) {
        this.isTransferInProgress = isTransferInProgress;
    }
}