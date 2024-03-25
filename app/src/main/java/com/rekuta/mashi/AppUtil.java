package com.rekuta.mashi;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.drawable.*;
import android.net.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

import java.io.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AppUtil {
    
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

    public static void hideKeyboard(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    public static void showKeyboard(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void showMessage(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }
    
    public static void showMessage(Context context, int r) {
        Toast.makeText(context, r, Toast.LENGTH_SHORT).show();
    }
    
    public static void checkThemePreference(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName().concat("_preferences"), Activity.MODE_PRIVATE);
        if (settings.getBoolean("darkMode", false)) {
            context.setTheme(R.style.AppThemeDark);
        } else {
            context.setTheme(R.style.AppThemeLight);
        }
    }

    public static ArrayList<HashMap<String, String>> getFromJSONArray(String string) {
        ArrayList<HashMap<String, String>> resultList = new ArrayList<>();
        
        try {
        	JSONArray jsonArray = new JSONArray(string);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSONArray objKeys = jsonObject.names();
                HashMap<String, String> map = new HashMap<>();

                for (int j = 0; j < jsonObject.length(); j++) {
                    String key = objKeys.getString(j);
                    String value = jsonObject.opt(key).toString();
                    map.put(key, value);
                }

                resultList.add(map);
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
