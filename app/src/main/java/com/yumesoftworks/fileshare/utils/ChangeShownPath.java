package com.yumesoftworks.fileshare.utils;

import android.util.Log;

public class ChangeShownPath {
    public void ChangeShownPath(){
    }

    public String filterString(String recPath){
        //divide the path
        String[] storageDivided=recPath.split("/");

        //check if it is the internal storage or the others
        int depthCounter=0;
        int finalDepth=2;
        for (String storage:storageDivided) {
            Log.d("aa",storage);
            if (storage.toLowerCase().contains("self") || storage.toLowerCase().contains("emulated") && depthCounter<=2){
                //internal storage
                Log.d("aa","internal");
                finalDepth=3;
            }
            depthCounter++;
        }

        String finalPath="/";
        depthCounter=0;

        for (String storage: storageDivided) {
            Log.d("aa",storage);
            if (depthCounter>finalDepth) {
                finalPath += storage+"/";
            }
            depthCounter++;
        }

        Log.d("aa",finalPath);

        return finalPath;
    }
}
