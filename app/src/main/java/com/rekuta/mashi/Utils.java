package com.rekuta.mashi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    
    public static String copyFromInputStream(InputStream input, String encoding) {
        char[] buffer = new char[1024];
        StringBuilder out = new StringBuilder();
        try {
        	Reader in = new InputStreamReader(input, encoding);
            for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                out.append(buffer, 0, numRead);
            }
            input.close();
        } catch(Exception err) {
            err.printStackTrace();
        }
        return out.toString();
    }

    public static void showMessage(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }
    
    public static void showMessage(Context context, int r) {
        Toast.makeText(context, r, Toast.LENGTH_SHORT).show();
    }
    
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName().concat("_preferences"), Activity.MODE_PRIVATE);
    }
    
    public static void checkThemePreference(Context context) {
        SharedPreferences settings = getDefaultSharedPreferences(context);
        if (settings.getBoolean("darkMode", false)) {
            context.setTheme(R.style.AppThemeDark);
        } else {
            context.setTheme(R.style.AppThemeLight);
        }
    }

    public static ArrayList<String[]> getFromJSONArray(String string) {
        ArrayList<String[]> resultList = new ArrayList<>();
        
        try {
        	JSONArray jsonArray = new JSONArray(string);

            for (int i = 0; i < jsonArray.length(); i++) {
                //JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSONArray jsonStrArray = jsonArray.getJSONArray(i);
                
                String[] strArr = new String[2];

                for (int j = 0; j < jsonStrArray.length(); j++) {
                    strArr[j] = jsonStrArray.getString(j);
                }

                resultList.add(strArr);
            }
        } catch(JSONException err) {
        	err.printStackTrace();
        }

        return resultList;
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    
}
