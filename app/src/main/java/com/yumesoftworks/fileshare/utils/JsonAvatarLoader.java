package com.yumesoftworks.fileshare.utils;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class JsonAvatarLoader {
    private static final String jsonUrl="https://www.yumesoftworks.com/fileshare/avatars.json";
    private static final String TAG="JsonUtils";

    public void JsonUtils(){
    }

    //load the json file
    public static String loadJson() throws IOException {
        Uri uri=Uri.parse(jsonUrl).buildUpon().build();
        URL url=new URL(uri.toString());
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();

        try {
            InputStream avatarList = connection.getInputStream();

            Scanner scanner = new Scanner(avatarList);
            scanner.useDelimiter("\\A");
            String finalString = "";

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                do {
                    String toAdd = scanner.next();
                    finalString =finalString.concat(toAdd);
                    hasInput = scanner.hasNext();
                } while (hasInput);
                //} while (hasInput == true);
                Log.d(TAG,"JSON successfully loaded");
                return finalString;
            } else {
                return null;
            }
        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            connection.disconnect();
        }
    }
}
