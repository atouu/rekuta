package com.rekuta.mashi.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.rekuta.mashi.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public static List<Float> byteArrayToFloatList(byte[] bytes) {
        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("Byte array length must be a multiple of 4.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes); // Match the byte order used during writing
        List<Float> floats = new ArrayList<>(); //float[ampData.length / 4];

        for (int i = 0; i < (bytes.length / 4); i++) {
            floats.add(buffer.getFloat());
        }

        return floats;
    }

    public static void showMessage(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    public static void showMessage(Context context, int r) {
        Toast.makeText(context, r, Toast.LENGTH_SHORT).show();
    }
    
    public static void checkThemePreference(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        switch (settings.getString(context.getString(R.string.pref_theme), context.getString(R.string.pref_theme_auto))) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static ArrayList<String[]> getFromJSONArray(String string) {
        ArrayList<String[]> resultList = new ArrayList<>();
        
        try {
        	JSONArray jsonArray = new JSONArray(string);

            for (int i = 0; i < jsonArray.length(); i++) {
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

    public static void setKeyboardDown(Activity context, MotionEvent event, EditText editText) {
        View v = context.getCurrentFocus();
        Rect outRect = new Rect();
        editText.getGlobalVisibleRect(outRect);
        if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
            assert v != null;
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            editText.clearFocus();
        }
    }

    public static void filePicker(Context context, String title, String[] extensions, DialogSelectionListener dialogSelection) {
        DialogProperties properties = new DialogProperties();
        properties.extensions = extensions;
        properties.show_hidden_files = true;

        FilePickerDialog dialog = new FilePickerDialog(context, properties);
        dialog.setTitle(title);
        dialog.setDialogSelectionListener(dialogSelection);
        dialog.show();
    }

    public static HashMap<String, Integer> parseBGMParam(String path) throws Exception {
        HashMap<String, Integer> params = new HashMap<>();
        String unit = "msec";

        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.matches("msec|MSEC|sec|SEC")) {
                unit = trimmed.toLowerCase();
                continue;
            }

            String[] parts = line.split(",", 7);
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }

            double duration = Double.parseDouble(parts[1]);
            int msec = (int) Math.round(unit.equals("sec") ? duration * 1000 : duration);

            if (Integer.parseInt(parts[2]) == 1) {
                params.put("rStart", msec);
            } else if (Integer.parseInt(parts[3]) == 1) {
                params.put("rStop", msec);
            }
        }

        return params;
    }
    
}
