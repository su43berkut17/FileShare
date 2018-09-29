package com.yumesoftworks.fileshare.data;

import java.util.List;

public class AvatarAndVersion {
    private int version;
    private List<AvatarStaticEntry> avatarList;

    public AvatarAndVersion(int version, List<AvatarStaticEntry> avatarList){
        this.version=version;
        this.avatarList=avatarList;
    }

    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }

    public List<AvatarStaticEntry> getAvatarList() {
        return avatarList;
    }
    public void setAvatarList(List<AvatarStaticEntry> avatarList) {
        this.avatarList = avatarList;
    }
}
