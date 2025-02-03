package com.rekuta.mashi;

import android.app.Application;

import com.rekuta.mashi.utilities.Utils;

public class RekutaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.checkThemePreference(this);
    }

}