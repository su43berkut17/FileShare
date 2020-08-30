package com.yumesoftworks.fileshare.data;

import java.util.ArrayList;
import java.util.List;

public class AvatarDefaultImages {
    private static List<AvatarStaticEntry> mAvatarList;

    public static List<AvatarStaticEntry> getDefaultImages(){
        mAvatarList=new ArrayList<>();

        for (int i=0; i<12;i++){
            String path="avatar_"+Integer.toString(i+1);

            mAvatarList.add(new AvatarStaticEntry(i,AvatarStaticEntry.TYPE_LOCAL,path,false));
        }

        return mAvatarList;
    }
}
