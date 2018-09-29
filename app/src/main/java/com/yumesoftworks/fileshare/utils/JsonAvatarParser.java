package com.yumesoftworks.fileshare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.yumesoftworks.fileshare.data.AvatarAndVersion;
import com.yumesoftworks.fileshare.data.AvatarStaticEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonAvatarParser {
    private AvatarAndVersion avatarAndVersion;
    private Context context;
    private static final String TAG="JsonAvatarParser";

    public JsonAvatarParser(Context contextSent){
        context=contextSent;
        loadData();
    }

    public void loadData(){
        new AsyncTask<Void,Void, AvatarAndVersion>(){

            @Override
            protected AvatarAndVersion doInBackground(Void... voids) {
                //we check if there's internet
                ConnectivityManager cm =
                        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();

                if (isConnected){
                    //we load
                    try {
                        String AvatarListString = JsonAvatarLoader.loadJson();
                        Log.d(TAG,"before parsing "+AvatarListString);

                        return parse(AvatarListString);
                    }catch (IOException e){
                        e.printStackTrace();
                        return null;
                    }
                }else{
                    return null;
                }
            }

            @Override
            protected void onPostExecute(AvatarAndVersion avatarAndVersion) {
                super.onPostExecute(avatarAndVersion);

                //Todo: return via the interface
            }
        }.execute();
    }

    private AvatarAndVersion parse(String data){
        //we parse avatar list
        try {
            //create the array
            Log.d(TAG,"begin parsing");

            JSONObject avatarJSON = new JSONObject(data);

            List<AvatarStaticEntry> mAvatarList=new ArrayList<>();
            mAvatarList.clear();

            //get the array of avatars
            JSONArray jsonAvatarsArray=avatarJSON.getJSONArray("items");

            for (int j=0;j<jsonAvatarsArray.length();j++){
                   AvatarStaticEntry avatar=new AvatarStaticEntry(j
                           ,AvatarStaticEntry.TYPE_REMOTE
                           ,jsonAvatarsArray.getJSONObject(j).getString("url")
                    );
                   mAvatarList.add(avatar);
            }

            //add it all to the final objects
            int version=avatarJSON.getInt("version");
            avatarAndVersion=new AvatarAndVersion(version,mAvatarList);

            //print the avatar version
            Log.d(TAG,"the 1st item is "+avatarAndVersion.getAvatarList().get(0).getPath());
            Log.d(TAG,"The version loaded is: "+avatarAndVersion.getVersion());

            return avatarAndVersion;

        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }
}