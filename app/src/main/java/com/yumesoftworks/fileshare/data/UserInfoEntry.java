package com.yumesoftworks.fileshare.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity (tableName = "userInfo")
public class UserInfoEntry implements Serializable {
    private static final long serialversionUID = 129348938L;
    public static int TYPE_LOCAL=1001;
    public static int TYPE_REMOTE=1002;

    @PrimaryKey (autoGenerate = true)
    private int id;
    private String username;
    private int pickedAvatar;
    private int numberFilesTransferred;
    private int assetVersion;
    private int isTransferInProgress;
    private int transferTypeSendOrReceive;

    @Ignore
    public UserInfoEntry(String username, int pickedAvatar, int numberFilesTransferred, int assetVersion, int isTransferInProgress, int transferTypeSendOrReceive){
        this.username=username;
        this.pickedAvatar=pickedAvatar;
        this.numberFilesTransferred=numberFilesTransferred;
        this.assetVersion=assetVersion;
        this.isTransferInProgress=isTransferInProgress;
        this.transferTypeSendOrReceive=transferTypeSendOrReceive;
    }

    public UserInfoEntry(int id, String username, int pickedAvatar, int numberFilesTransferred, int assetVersion, int isTransferInProgress, int transferTypeSendOrReceive){
        this.id=id;
        this.username=username;
        this.pickedAvatar=pickedAvatar;
        this.numberFilesTransferred=numberFilesTransferred;
        this.assetVersion=assetVersion;
        this.isTransferInProgress=isTransferInProgress;
        this.transferTypeSendOrReceive=transferTypeSendOrReceive;
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

    public int getTransferTypeSendOrReceive() {
        return transferTypeSendOrReceive;
    }
    public void setTransferTypeSendOrReceive(int transferTypeSendOrReceive) {
        this.transferTypeSendOrReceive = transferTypeSendOrReceive;
    }
}